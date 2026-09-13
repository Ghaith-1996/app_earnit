package com.restlock.domain.fake

import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.WorkoutLog
import com.restlock.domain.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * In-memory implementation of [SessionEngine] for the frontend to run against
 * without the real Android services.
 *
 * It honours the exact contract documented on [SessionEngine] so swapping in
 * the real backend (AlarmManager + AccessibilityService) is a drop-in.
 */
class FakeSessionEngine(
    private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
) : SessionEngine {

    private val _state = MutableStateFlow(
        SessionState(
            phase = SessionState.Phase.Idle,
            chosenRest = SettingsRepository.DefaultRest,
        )
    )
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private var ticker: Job? = null

    override fun startWorkout(rest: Duration, workout: PlannedWorkout?) {
        if (_state.value.isInSession || workout?.exercises?.isEmpty() == true) return
        cancelTicker()
        _state.value = SessionState(
            phase = SessionState.Phase.Resting,
            chosenRest = rest,
            remaining = rest,
            setsCompleted = 0,
            extraRests = 0,
            blockerArmed = false,
            plannedSets = workout?.totalSets,
        )
        startCountdown(rest, onExpire = { armDecision() })
    }

    override fun addThirtySeconds() {
        val current = _state.value
        if (current.phase != SessionState.Phase.AwaitingDecision) return
        cancelTicker()
        _state.value = current.copy(
            phase = SessionState.Phase.Resting,
            remaining = 30.seconds,
            extraRests = current.extraRests + 1,
            blockerArmed = false,
        )
        startCountdown(30.seconds, onExpire = { armDecision() })
    }

    override fun exerciseDone(onFinished: (WorkoutLog?) -> Unit) {
        val current = _state.value
        if (current.phase != SessionState.Phase.AwaitingDecision) return
        if (current.isFinalSet) {
            finishWorkout(onFinished)
            return
        }
        cancelTicker()
        _state.value = current.copy(
            phase = SessionState.Phase.Resting,
            remaining = current.chosenRest,
            setsCompleted = current.setsCompleted + 1,
            blockerArmed = false,
        )
        startCountdown(current.chosenRest, onExpire = { armDecision() })
    }

    override fun finishWorkout(onFinished: (WorkoutLog?) -> Unit) {
        cancelTicker()
        val previous = _state.value
        _state.value = SessionState(
            phase = SessionState.Phase.Idle,
            chosenRest = previous.chosenRest,
            remaining = null,
            setsCompleted = 0,
            extraRests = 0,
            blockerArmed = false,
        )
        // Refresh chosenRest from persisted settings so the next session uses
        // whatever the user chose most recently.
        scope.launch {
            val saved = settingsRepository.chosenRest.first()
            _state.value = _state.value.copy(chosenRest = saved)
        }
        onFinished(null)
    }

    private fun armDecision() {
        _state.value = _state.value.copy(
            phase = SessionState.Phase.AwaitingDecision,
            remaining = null,
            blockerArmed = true,
        )
    }

    private fun startCountdown(total: Duration, onExpire: () -> Unit) {
        ticker = scope.launch {
            var remaining = total
            val step = 250.milliseconds
            while (remaining > Duration.ZERO) {
                _state.value = _state.value.copy(remaining = remaining)
                delay(step)
                remaining -= step
            }
            _state.value = _state.value.copy(remaining = Duration.ZERO)
            onExpire()
        }
    }

    private fun cancelTicker() {
        ticker?.cancel()
        ticker = null
    }
}
