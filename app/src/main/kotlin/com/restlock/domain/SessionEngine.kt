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
 *    ├─ exerciseDone()            ──▶ Resting (remaining = chosenRest, setsCompleted +1)
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

    fun startWorkout(rest: Duration)
    fun addThirtySeconds()
    fun exerciseDone()
    fun finishWorkout()
}
