package com.restlock.domain

import kotlin.time.Duration

/**
 * The full snapshot of the workout session as observed by the UI.
 *
 * This is the single source of truth the [SessionEngine] streams to the UI.
 * The UI never derives "what state am I in" from anything else.
 */
data class SessionState(
    val phase: Phase,
    /** Wall-clock duration the user picked for normal rests, e.g. 90s. */
    val chosenRest: Duration,
    /** Remaining time on the active countdown, or null when no countdown is active. */
    val remaining: Duration? = null,
    /** Sets the user has marked as "Exercise done" in this session. */
    val setsCompleted: Int = 0,
    /** How many times the user added a 30s rest extension in this session. */
    val extraRests: Int = 0,
    /** True while the lock is actively suppressing blocked apps (only in AwaitingDecision). */
    val blockerArmed: Boolean = false,
) {
    enum class Phase { Idle, Resting, AwaitingDecision }

    val isInSession: Boolean get() = phase != Phase.Idle
}
