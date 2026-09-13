package com.restlock.domain

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * The state machine the UI observes and commands.
 *
 * Lifecycle expectations (these are the same on the fake and the real impl):
 *
 *  Idle
 *    └─ startWorkout(restSeconds) ──▶ Resting (remaining = restSeconds)
 *
 *  Resting
 *    ├─ timer ticks down to 0     ──▶ AwaitingDecision (blockerArmed = true)
 *    └─ finishWorkout()           ──▶ Idle
 *
 *  AwaitingDecision
 *    ├─ exerciseDone()            ──▶ Resting (setsCompleted +1), or Idle on the final planned set
 *    ├─ addThirtySeconds()        ──▶ Resting (remaining = 30s, extraRests +1, blockerArmed = false)
 *    └─ finishWorkout()           ──▶ Idle
 *
 * Implementations MUST guarantee:
 *  - [state] emits the latest snapshot on every meaningful change, including
 *    every countdown tick the UI needs to render.
 *  - Process death is survivable: the next collector after restart sees the
 *    correct phase (the backend agent persists this via DataStore).
 */
interface SessionEngine {
    val state: StateFlow<SessionState>

    /** A saved plan starts at set 1; null starts an open-ended quick session. Active sessions are preserved. */
    fun startWorkout(rest: Duration, workout: PlannedWorkout? = null)
    fun addThirtySeconds()
    /** The callback runs only on final-set completion, with the saved result if available. */
    fun exerciseDone(onFinished: (WorkoutLog?) -> Unit = {})
    fun finishWorkout(onFinished: (WorkoutLog?) -> Unit = {})
}
