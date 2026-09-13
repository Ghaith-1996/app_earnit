package com.fitness.restlock.backend

enum class WorkoutMode {
    Idle,
    Resting,
    AwaitingDecision,
}

data class PermissionStatus(
    val isAccessibilityServiceEnabled: Boolean = false,
    val canScheduleExactAlarms: Boolean = true,
    val needsExactAlarmPermission: Boolean = false,
) {
    val isAppBlockingReady: Boolean
        get() = isAccessibilityServiceEnabled
}

data class WorkoutState(
    val mode: WorkoutMode = WorkoutMode.Idle,
    val restDurationSeconds: Int = DEFAULT_REST_DURATION_SECONDS,
    val remainingSeconds: Int = 0,
    val allowedApps: Set<String> = emptySet(),
    val permissionStatus: PermissionStatus = PermissionStatus(),
    val completedSets: Int = 0,
    val extraRests: Int = 0,
    val plannedSets: Int? = null,
) {
    val isWorkoutActive: Boolean
        get() = mode != WorkoutMode.Idle

    @Deprecated(
        message = "The app now uses an allowlist. Read allowedApps instead.",
        replaceWith = ReplaceWith("allowedApps"),
    )
    val blockedApps: Set<String>
        get() = allowedApps

    companion object {
        const val DEFAULT_REST_DURATION_SECONDS = 90
    }
}
