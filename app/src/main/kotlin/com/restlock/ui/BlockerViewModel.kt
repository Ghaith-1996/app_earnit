package com.restlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restlock.domain.FitnessRepository
import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import com.restlock.domain.ActiveExercisePreview
import com.restlock.domain.WorkoutProgress
import com.restlock.domain.WorkoutLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface BlockerCompletion {
    data object None : BlockerCompletion
    data object Saving : BlockerCompletion
    data class Finished(val log: WorkoutLog?) : BlockerCompletion
}

class BlockerViewModel(
    private val sessionEngine: SessionEngine,
    private val fitnessRepository: FitnessRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionEngine.state
    val pendingWorkoutSummary = fitnessRepository.pendingWorkoutSummary.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )
    private val mutableCompletion = MutableStateFlow<BlockerCompletion>(BlockerCompletion.None)
    val completion = mutableCompletion.asStateFlow()
    val activeExercisePreview: StateFlow<ActiveExercisePreview?> = combine(
        sessionState, fitnessRepository.savedWorkouts, fitnessRepository.activeWorkoutId,
    ) { session, workouts, activeId ->
        val workout = workouts.firstOrNull { session.isInSession && it.id == activeId }
        WorkoutProgress.activeExercise(workout, session.setsCompleted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun exerciseDone(onFinished: (WorkoutLog?) -> Unit = {}) {
        if (sessionState.value.phase == SessionState.Phase.AwaitingDecision && sessionState.value.isFinalSet) {
            mutableCompletion.value = BlockerCompletion.Saving
        }
        sessionEngine.exerciseDone { log ->
            mutableCompletion.value = BlockerCompletion.Finished(log)
            onFinished(log)
        }
    }
    fun addThirtySeconds() = sessionEngine.addThirtySeconds()
    fun finishWorkout(onFinished: (WorkoutLog?) -> Unit = {}) {
        mutableCompletion.value = BlockerCompletion.Saving
        sessionEngine.finishWorkout { log ->
            mutableCompletion.value = BlockerCompletion.Finished(log)
            onFinished(log)
        }
    }
}
