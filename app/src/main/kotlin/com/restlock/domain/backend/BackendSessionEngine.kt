package com.restlock.domain.backend

import com.fitness.restlock.backend.WorkoutController
import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutState
import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import com.restlock.domain.FitnessRepository
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.ExerciseCatalog
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
            val saved = if (workout == null) null else {
                fitnessRepository.savedWorkouts.first().firstOrNull { it.id == workout.id }
                    ?: return@trySend
            }
            if (saved != null && (saved.exercises.isEmpty() ||
                    saved.exercises.any { ExerciseCatalog.byId(it.exerciseId) == null })) return@trySend
            if (controller.startWorkout(rest.inWholeSeconds.toInt(), saved?.totalSets)) {
                try {
                    fitnessRepository.setActiveWorkoutId(saved?.id)
                } catch (error: Exception) {
                    controller.finishWorkout()
                    throw error
                }
            }
        }
    }

    override fun addThirtySeconds() {
        commands.trySend { controller.addThirtySecondsRest() }
    }

    override fun exerciseDone() {
        commands.trySend {
            if (controller.exerciseDone()) clearFinishedWorkout()
        }
    }

    override fun finishWorkout(onFinished: () -> Unit) {
        commands.trySend {
            // End blocking and cancel the alarm before optional existing logging.
            controller.finishWorkout()
            clearFinishedWorkout()
            withContext(Dispatchers.Main.immediate) { onFinished() }
        }
    }

    private suspend fun clearFinishedWorkout() {
        try {
            fitnessRepository.logActiveWorkoutAndClear(System.currentTimeMillis())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            fitnessRepository.setActiveWorkoutId(null)
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
