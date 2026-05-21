package com.fitness.restlock.backend.session

import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutState

data class WorkoutSessionSnapshot(
    val mode: WorkoutMode = WorkoutMode.Idle,
    val restDurationSeconds: Int = WorkoutState.DEFAULT_REST_DURATION_SECONDS,
    val allowedApps: Set<String> = emptySet(),
    val timerEndEpochMillis: Long = 0L,
    val completedSets: Int = 0,
    val extraRests: Int = 0,
) {
    @Deprecated(
        message = "The app now uses an allowlist. Read allowedApps instead.",
        replaceWith = ReplaceWith("allowedApps"),
    )
    val blockedApps: Set<String>
        get() = allowedApps
}
