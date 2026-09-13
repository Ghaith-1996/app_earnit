package com.restlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restlock.domain.FitnessRepository
import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import com.restlock.domain.ActiveExercisePreview
import com.restlock.domain.WorkoutProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow

class BlockerViewModel(
    private val sessionEngine: SessionEngine,
    private val fitnessRepository: FitnessRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionEngine.state
    val activeExercisePreview: StateFlow<ActiveExercisePreview?> = combine(
        sessionState, fitnessRepository.savedWorkouts, fitnessRepository.activeWorkoutId,
    ) { session, workouts, activeId ->
        val workout = workouts.firstOrNull { session.isInSession && it.id == activeId }
        WorkoutProgress.activeExercise(workout, session.setsCompleted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun exerciseDone() = sessionEngine.exerciseDone()
    fun addThirtySeconds() = sessionEngine.addThirtySeconds()
    fun finishWorkout(onFinished: () -> Unit = {}) {
        sessionEngine.finishWorkout(onFinished)
    }
}
