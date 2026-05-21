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

    fun startWorkout()
    fun addThirtySecondsRest()
    fun exerciseDone()
    fun finishWorkout()
}
