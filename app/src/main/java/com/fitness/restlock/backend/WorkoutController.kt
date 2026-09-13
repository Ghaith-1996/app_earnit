package com.fitness.restlock.backend

import kotlinx.coroutines.flow.StateFlow

/** Captured from the persisted transition, before the live counters return to Idle. */
data class WorkoutCompletion(val completedSets: Int, val completedAtMillis: Long)
data class WorkoutStart(val workoutId: String, val startedAtMillis: Long)

interface WorkoutController {
    val state: StateFlow<WorkoutState>

    fun setRestDuration(seconds: Int)
    fun setAllowedApps(packages: Set<String>)

    @Deprecated(
        message = "The app now uses an allowlist. Call setAllowedApps instead.",
        replaceWith = ReplaceWith("setAllowedApps(packages)"),
    )
    fun setBlockedApps(packages: Set<String>) = setAllowedApps(packages)

    /** Returns false if another session is already active. */
    suspend fun startWorkout(restSeconds: Int, plannedSets: Int? = null, workoutId: String? = null): Boolean
    /** Persisted identity while running, for recovery before the fitness store has been updated. */
    suspend fun activeWorkout(): WorkoutStart?
    fun addThirtySecondsRest()
    /** Returns a result only when this command completed the final planned set. */
    suspend fun exerciseDone(): WorkoutCompletion?
    suspend fun finishWorkout(): WorkoutCompletion
    suspend fun pendingCompletion(): WorkoutCompletion?
    suspend fun acknowledgeCompletion()
}
