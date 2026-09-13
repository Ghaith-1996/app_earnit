package com.fitness.restlock.backend.session

import com.fitness.restlock.backend.PermissionStatus
import com.fitness.restlock.backend.WorkoutController
import com.fitness.restlock.backend.WorkoutCompletion
import com.fitness.restlock.backend.WorkoutStart
import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutState
import com.fitness.restlock.backend.alarm.RestAlarmScheduler
import com.fitness.restlock.backend.blocking.AppBlockPolicy
import com.fitness.restlock.backend.permissions.PermissionGateway
import com.fitness.restlock.backend.persistence.WorkoutPreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DefaultWorkoutController(
    private val store: WorkoutPreferencesStore,
    private val alarmScheduler: RestAlarmScheduler,
    private val permissionGateway: PermissionGateway,
    private val blockPolicy: AppBlockPolicy,
    private val scope: CoroutineScope,
    private val clock: Clock = SystemClock,
) : WorkoutController {
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val ticker = MutableStateFlow(clock.nowMillis())

    override val state: StateFlow<WorkoutState> = combine(
        store.snapshots,
        ticker,
    ) { snapshot, nowMillis ->
        snapshot.toWorkoutState(nowMillis, permissionGateway.currentStatus())
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = WorkoutSessionSnapshot()
            .toWorkoutState(clock.nowMillis(), permissionGateway.currentStatus()),
    )

    init {
        scope.launch {
            for (command in commands) {
                runCatching { command() }
            }
        }

        scope.launch {
            while (isActive) {
                ticker.value = clock.nowMillis()
                delay(TICK_INTERVAL_MILLIS)
            }
        }

        scope.launch {
            combine(store.snapshots, ticker) { snapshot, nowMillis -> snapshot to nowMillis }
                .collect { (snapshot, nowMillis) ->
                    if (snapshot.mode == WorkoutMode.Resting &&
                        snapshot.timerEndEpochMillis > 0L &&
                        snapshot.timerEndEpochMillis <= nowMillis
                    ) {
                        enqueue {
                            persistAndSyncAlarm {
                                WorkoutSessionReducer.expireIfNeeded(it, nowMillis)
                            }
                        }
                    }
                }
        }
    }

    override fun setRestDuration(seconds: Int) {
        enqueue {
            persistAndSyncAlarm {
                WorkoutSessionReducer.setRestDuration(it, seconds)
            }
        }
    }

    override fun setAllowedApps(packages: Set<String>) {
        enqueue {
            val sanitized = blockPolicy.sanitizeAllowedPackages(packages)
            persistAndSyncAlarm {
                WorkoutSessionReducer.setAllowedApps(it, sanitized)
            }
        }
    }

    override suspend fun startWorkout(restSeconds: Int, plannedSets: Int?, workoutId: String?): Boolean {
        return execute {
            val nowMillis = clock.nowMillis()
            var started = false
            persistAndSyncAlarm {
                if (it.mode != WorkoutMode.Idle || it.pendingCompletion != null ||
                    (plannedSets != null && plannedSets <= 0)) {
                    it
                } else {
                    started = true
                    WorkoutSessionReducer.startWorkout(
                        WorkoutSessionReducer.setRestDuration(it, restSeconds), nowMillis, plannedSets,
                    ).copy(activeWorkout = workoutId?.let { id -> WorkoutStart(id, nowMillis) })
                }
            }
            started
        }
    }

    override fun addThirtySecondsRest() {
        enqueue {
            val nowMillis = clock.nowMillis()
            persistAndSyncAlarm {
                WorkoutSessionReducer.addThirtySecondsRest(it, nowMillis)
            }
        }
    }

    override suspend fun exerciseDone(): WorkoutCompletion? {
        return execute {
            val nowMillis = clock.nowMillis()
            var completion: WorkoutCompletion? = null
            persistAndSyncAlarm {
                val next = WorkoutSessionReducer.exerciseDone(it, nowMillis)
                if (it.mode == WorkoutMode.AwaitingDecision && next.mode == WorkoutMode.Idle) {
                    completion = WorkoutCompletion(it.completedSets + 1, nowMillis)
                }
                next.copy(pendingCompletion = completion ?: it.pendingCompletion)
            }
            completion
        }
    }

    override suspend fun finishWorkout(): WorkoutCompletion {
        return execute {
            var completion = WorkoutCompletion(0, clock.nowMillis())
            persistAndSyncAlarm {
                completion = it.pendingCompletion ?: WorkoutCompletion(it.completedSets, clock.nowMillis())
                WorkoutSessionReducer.finishWorkout(it).copy(pendingCompletion = completion)
            }
            completion
        }
    }

    fun handleRestTimerAlarm(): Job {
        return scope.launch {
            val completed = CompletableDeferred<Unit>()
            enqueue {
                try {
                    val nowMillis = clock.nowMillis()
                    persistAndSyncAlarm {
                        WorkoutSessionReducer.expireIfNeeded(it, nowMillis)
                    }
                } finally {
                    completed.complete(Unit)
                }
            }
            completed.await()
        }
    }

    private fun enqueue(command: suspend () -> Unit) {
        commands.trySend(command)
    }

    override suspend fun pendingCompletion(): WorkoutCompletion? = store.snapshots.first().pendingCompletion

    override suspend fun activeWorkout(): WorkoutStart? = store.snapshots.first().let {
        if (it.mode == WorkoutMode.Idle) null else it.activeWorkout
    }

    override suspend fun acknowledgeCompletion() {
        execute { store.update { it.copy(pendingCompletion = null, activeWorkout = null) } }
    }

    private suspend fun <T> execute(command: suspend () -> T): T {
        val result = CompletableDeferred<T>()
        enqueue {
            try {
                result.complete(command())
            } catch (error: Throwable) {
                result.completeExceptionally(error)
            }
        }
        return result.await()
    }

    private suspend fun persistAndSyncAlarm(
        transform: (WorkoutSessionSnapshot) -> WorkoutSessionSnapshot,
    ) {
        val next = store.update(transform)
        syncAlarm(next)
    }

    private fun syncAlarm(snapshot: WorkoutSessionSnapshot) {
        if (snapshot.mode == WorkoutMode.Resting &&
            snapshot.timerEndEpochMillis > clock.nowMillis()
        ) {
            alarmScheduler.scheduleRestEnd(snapshot.timerEndEpochMillis)
        } else {
            alarmScheduler.cancelRestEnd()
        }
    }

    private fun WorkoutSessionSnapshot.toWorkoutState(
        nowMillis: Long,
        permissionStatus: PermissionStatus,
    ): WorkoutState {
        val effectiveSnapshot = WorkoutSessionReducer.expireIfNeeded(this, nowMillis)
        return WorkoutState(
            mode = effectiveSnapshot.mode,
            restDurationSeconds = effectiveSnapshot.restDurationSeconds,
            remainingSeconds = WorkoutSessionReducer.remainingSeconds(
                effectiveSnapshot,
                nowMillis,
            ),
            allowedApps = effectiveSnapshot.allowedApps,
            permissionStatus = permissionStatus,
            completedSets = effectiveSnapshot.completedSets,
            extraRests = effectiveSnapshot.extraRests,
            plannedSets = effectiveSnapshot.plannedSets,
        )
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 1_000L
    }
}
