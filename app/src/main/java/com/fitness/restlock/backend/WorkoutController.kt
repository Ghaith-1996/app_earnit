package com.fitness.restlock.backend

import kotlinx.coroutines.flow.StateFlow

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
    suspend fun startWorkout(restSeconds: Int, plannedSets: Int? = null): Boolean
    fun addThirtySecondsRest()
    /** Returns true when this command completed the final planned set. */
    suspend fun exerciseDone(): Boolean
    suspend fun finishWorkout()
}
