package com.restlock.domain.backend

import com.fitness.restlock.backend.WorkoutController
import com.fitness.restlock.backend.WorkoutCompletion
import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutState
import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import com.restlock.domain.FitnessRepository
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.ActiveWorkoutSession
import com.restlock.domain.WorkoutLog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class BackendSessionEngine(
    private val controller: WorkoutController,
    private val fitnessRepository: FitnessRepository,
    private val scope: CoroutineScope,
) : SessionEngine {
    // Keep selection, session commands and completion bookkeeping ordered across both activities.
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        commands.trySend {
            fitnessRepository.withWorkoutStartLock { restoreActiveWorkout() }
            controller.pendingCompletion()?.let { clearFinishedWorkout(it) }
        }
        scope.launch {
            for (command in commands) {
                // A storage failure must not disable every subsequent session command.
                runCatching { command() }.onFailure {
                    if (it is CancellationException) throw it
                }
            }
        }
    }

    override val state: StateFlow<SessionState> = controller.state
        .map { it.toSessionState() }
        .stateIn(
            scope = scope,
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
            initialValue = controller.state.value.toSessionState(),
        )

    override fun startWorkout(rest: Duration, workout: PlannedWorkout?) {
        commands.trySend {
            fitnessRepository.withWorkoutStartLock {
                controller.pendingCompletion()?.let { clearFinishedWorkout(it) }
                if (controller.pendingCompletion() != null) return@withWorkoutStartLock
                val saved = if (workout == null) null else {
                    val current = fitnessRepository.savedWorkouts.first().firstOrNull { it.id == workout.id }
                        ?: return@withWorkoutStartLock
                    current.copy(exercises = current.orderedExercises)
                }
                if (saved != null && saved.exercises.isEmpty()) return@withWorkoutStartLock
                // Keep the chosen definition stable until its active ID is persisted.
                if (controller.startWorkout(rest.inWholeSeconds.toInt(), saved?.totalSets, saved?.id)) {
                    try {
                        val active = controller.activeWorkout()
                        fitnessRepository.setActiveWorkoutSession(active?.let { ActiveWorkoutSession(it.workoutId, it.startedAtMillis) })
                    } catch (error: Exception) {
                        controller.finishWorkout()
                        throw error
                    }
                }
            }
        }
    }

    override fun addThirtySeconds() {
        commands.trySend { controller.addThirtySecondsRest() }
    }

    override fun exerciseDone(onFinished: (WorkoutLog?) -> Unit) {
        commands.trySend {
            controller.exerciseDone()?.let { completion ->
                val log = clearFinishedWorkout(completion)
                withContext(Dispatchers.Main.immediate) { onFinished(log) }
            }
        }
    }

    override fun finishWorkout(onFinished: (WorkoutLog?) -> Unit) {
        commands.trySend {
            // End blocking and cancel the alarm before optional existing logging.
            val completion = controller.finishWorkout()
            val log = clearFinishedWorkout(completion)
            withContext(Dispatchers.Main.immediate) { onFinished(log) }
        }
    }

    private suspend fun restoreActiveWorkout() {
        // The backend commits this identity with the start transition. Only restore while
        // running: after a finish, a cleared fitness ID means the result was already saved.
        controller.activeWorkout()?.let {
            fitnessRepository.setActiveWorkoutSession(ActiveWorkoutSession(it.workoutId, it.startedAtMillis))
        }
    }

    private suspend fun clearFinishedWorkout(completion: WorkoutCompletion): WorkoutLog? {
        return try {
            val log = fitnessRepository.logActiveWorkoutAndClear(completion.completedAtMillis, completion.completedSets)
            controller.acknowledgeCompletion()
            log
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Keep the persisted receipt and selection for a retry; blocking has already ended.
            null
        }
    }

    private fun WorkoutState.toSessionState(): SessionState {
        return SessionState(
            phase = when (mode) {
                WorkoutMode.Idle -> SessionState.Phase.Idle
                WorkoutMode.Resting -> SessionState.Phase.Resting
                WorkoutMode.AwaitingDecision -> SessionState.Phase.AwaitingDecision
            },
            chosenRest = restDurationSeconds.seconds,
            remaining = if (mode == WorkoutMode.Resting) remainingSeconds.seconds else null,
            setsCompleted = completedSets,
            extraRests = extraRests,
            blockerArmed = mode == WorkoutMode.AwaitingDecision,
            plannedSets = plannedSets,
        )
    }
}
