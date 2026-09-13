package com.fitness.restlock.backend.session

import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutState
import com.fitness.restlock.backend.WorkoutCompletion
import com.fitness.restlock.backend.WorkoutStart

data class WorkoutSessionSnapshot(
    val mode: WorkoutMode = WorkoutMode.Idle,
    val restDurationSeconds: Int = WorkoutState.DEFAULT_REST_DURATION_SECONDS,
    val allowedApps: Set<String> = emptySet(),
    val timerEndEpochMillis: Long = 0L,
    val completedSets: Int = 0,
    val extraRests: Int = 0,
    val plannedSets: Int? = null,
    val pendingCompletion: WorkoutCompletion? = null,
    val activeWorkout: WorkoutStart? = null,
) {
    @Deprecated(
        message = "The app now uses an allowlist. Read allowedApps instead.",
        replaceWith = ReplaceWith("allowedApps"),
    )
    val blockedApps: Set<String>
        get() = allowedApps
}
