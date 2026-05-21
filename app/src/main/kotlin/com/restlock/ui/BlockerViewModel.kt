package com.restlock.ui

import androidx.lifecycle.ViewModel
import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import kotlinx.coroutines.flow.StateFlow

class BlockerViewModel(
    private val sessionEngine: SessionEngine,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = sessionEngine.state

    fun exerciseDone() = sessionEngine.exerciseDone()
    fun addThirtySeconds() = sessionEngine.addThirtySeconds()
    fun finishWorkout() = sessionEngine.finishWorkout()
}
