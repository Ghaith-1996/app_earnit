package com.restlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restlock.domain.FitnessRepository
import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BlockerViewModel(
    private val sessionEngine: SessionEngine,
    private val fitnessRepository: FitnessRepository,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionEngine.state

    fun exerciseDone() = sessionEngine.exerciseDone()
    fun addThirtySeconds() = sessionEngine.addThirtySeconds()
    fun finishWorkout(onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                fitnessRepository.logActiveWorkoutAndClear(System.currentTimeMillis())
            }
            sessionEngine.finishWorkout()
            onFinished()
        }
    }
}
