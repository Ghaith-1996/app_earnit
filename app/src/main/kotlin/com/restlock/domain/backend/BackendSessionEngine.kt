package com.restlock.domain.backend

import com.fitness.restlock.backend.WorkoutController
import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutState
import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class BackendSessionEngine(
    private val controller: WorkoutController,
) : SessionEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val state: StateFlow<SessionState> = controller.state
        .map { it.toSessionState() }
        .stateIn(
            scope = scope,
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
            initialValue = controller.state.value.toSessionState(),
        )

    override fun startWorkout(rest: Duration) {
        controller.setRestDuration(rest.inWholeSeconds.toInt())
        controller.startWorkout()
    }

    override fun addThirtySeconds() {
        controller.addThirtySecondsRest()
    }

    override fun exerciseDone() {
        controller.exerciseDone()
    }

    override fun finishWorkout() {
        controller.finishWorkout()
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
        )
    }
}
