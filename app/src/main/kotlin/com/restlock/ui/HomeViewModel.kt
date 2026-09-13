package com.restlock.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.restlock.backend.PermissionStatus
import com.fitness.restlock.backend.blocking.BlockingDiagnostics
import com.fitness.restlock.backend.blocking.BlockingDiagnosticsState
import com.fitness.restlock.backend.permissions.PermissionGateway
import com.restlock.domain.FitnessRepository
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import com.restlock.domain.SettingsRepository
import com.restlock.domain.ActiveExercisePreview
import com.restlock.domain.UserProfile
import com.restlock.domain.WorkoutLog
import com.restlock.domain.WorkoutProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

data class HomePermissionState(
    val isAccessibilityServiceEnabled: Boolean,
    val needsExactAlarmPermission: Boolean,
    val canScheduleExactAlarms: Boolean,
) {
    val needsAction: Boolean
        get() = !isAccessibilityServiceEnabled
}

data class HomeUiState(
    val session: SessionState,
    val chosenRest: Duration,
    val allowedAppCount: Int,
    val permissions: HomePermissionState,
    val blockingDiagnostics: BlockingDiagnosticsState,
    val recentWorkoutLogs: List<WorkoutLog> = emptyList(),
    val userProfile: UserProfile = UserProfile(),
    val savedWorkouts: List<PlannedWorkout> = emptyList(),
    val activeWorkout: PlannedWorkout? = null,
    val activeExercisePreview: ActiveExercisePreview? = null,
)

private data class HomeBaseState(
    val session: SessionState,
    val chosenRest: Duration,
    val allowedAppCount: Int,
    val permissions: HomePermissionState,
    val blockingDiagnostics: BlockingDiagnosticsState,
)

class HomeViewModel(
    private val sessionEngine: SessionEngine,
    private val settingsRepository: SettingsRepository,
    private val fitnessRepository: FitnessRepository,
    private val permissionGateway: PermissionGateway,
    private val refreshPermissionsPeriodically: Boolean = true,
) : ViewModel() {
    private val permissions = MutableStateFlow(permissionGateway.currentStatus().toHomeState())
    val pendingWorkoutSummary = fitnessRepository.pendingWorkoutSummary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismissWorkoutSummary() {
        viewModelScope.launch { fitnessRepository.dismissWorkoutSummary() }
    }

    private val baseState = combine(
        sessionEngine.state,
        settingsRepository.chosenRest,
        settingsRepository.allowedPackages,
        permissions,
        BlockingDiagnostics.state,
    ) { session, rest, allowed, permissionState, diagnostics ->
        HomeBaseState(
            session = session,
            chosenRest = rest,
            allowedAppCount = allowed.size,
            permissions = permissionState,
            blockingDiagnostics = diagnostics,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseState,
        fitnessRepository.workoutLogs,
        fitnessRepository.userProfile,
        fitnessRepository.savedWorkouts,
        fitnessRepository.activeWorkoutId,
    ) { base, logs, profile, savedWorkouts, activeWorkoutId ->
        val activeWorkout = savedWorkouts.firstOrNull { base.session.isInSession && it.id == activeWorkoutId }
        HomeUiState(
            session = base.session,
            chosenRest = base.chosenRest,
            allowedAppCount = base.allowedAppCount,
            permissions = base.permissions,
            blockingDiagnostics = base.blockingDiagnostics,
            recentWorkoutLogs = logs.take(3),
            userProfile = profile,
            savedWorkouts = savedWorkouts,
            activeWorkout = activeWorkout,
            activeExercisePreview = WorkoutProgress.activeExercise(
                workout = activeWorkout,
                completedSets = base.session.setsCompleted,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            session = sessionEngine.state.value,
            chosenRest = SettingsRepository.DefaultRest,
            allowedAppCount = 0,
            permissions = permissions.value,
            blockingDiagnostics = BlockingDiagnostics.state.value,
            recentWorkoutLogs = emptyList(),
            userProfile = UserProfile(),
            savedWorkouts = emptyList(),
            activeWorkout = null,
        ),
    )

    init {
        if (refreshPermissionsPeriodically) {
            viewModelScope.launch {
                while (isActive) {
                    refreshPermissions()
                    delay(PERMISSION_REFRESH_MILLIS)
                }
            }
        }
    }

    fun startWorkout(rest: Duration) {
        sessionEngine.startWorkout(rest)
    }

    fun startSavedWorkout(workoutId: String, rest: Duration) {
        viewModelScope.launch {
            val workout = fitnessRepository.savedWorkouts.first().firstOrNull { it.id == workoutId }
                ?: return@launch
            sessionEngine.startWorkout(rest, workout)
        }
    }

    fun addThirtySeconds() = sessionEngine.addThirtySeconds()
    fun exerciseDone() = sessionEngine.exerciseDone()
    fun finishWorkout(onFinished: (WorkoutLog?) -> Unit = {}) {
        sessionEngine.finishWorkout(onFinished)
    }

    fun refreshPermissions() {
        permissions.value = permissionGateway.currentStatus().toHomeState()
    }

    fun accessibilitySettingsIntent(): Intent = permissionGateway.accessibilitySettingsIntent()

    private fun PermissionStatus.toHomeState(): HomePermissionState {
        return HomePermissionState(
            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled,
            needsExactAlarmPermission = needsExactAlarmPermission,
            canScheduleExactAlarms = canScheduleExactAlarms,
        )
    }

    private companion object {
        const val PERMISSION_REFRESH_MILLIS = 1_000L
    }
}
