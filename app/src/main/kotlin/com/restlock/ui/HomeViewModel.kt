package com.restlock.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.restlock.backend.PermissionStatus
import com.fitness.restlock.backend.blocking.BlockingDiagnostics
import com.fitness.restlock.backend.blocking.BlockingDiagnosticsState
import com.fitness.restlock.backend.permissions.PermissionGateway
import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import com.restlock.domain.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
)

class HomeViewModel(
    private val sessionEngine: SessionEngine,
    private val settingsRepository: SettingsRepository,
    private val permissionGateway: PermissionGateway,
) : ViewModel() {
    private val permissions = MutableStateFlow(permissionGateway.currentStatus().toHomeState())

    val uiState: StateFlow<HomeUiState> = combine(
        sessionEngine.state,
        settingsRepository.chosenRest,
        settingsRepository.allowedPackages,
        permissions,
        BlockingDiagnostics.state,
    ) { session, rest, allowed, permissionState, diagnostics ->
        HomeUiState(
            session = session,
            chosenRest = rest,
            allowedAppCount = allowed.size,
            permissions = permissionState,
            blockingDiagnostics = diagnostics,
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
        ),
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                refreshPermissions()
                delay(PERMISSION_REFRESH_MILLIS)
            }
        }
    }

    fun startWorkout(rest: Duration) {
        viewModelScope.launch {
            settingsRepository.setChosenRest(rest)
            sessionEngine.startWorkout(rest)
        }
    }

    fun addThirtySeconds() = sessionEngine.addThirtySeconds()
    fun exerciseDone() = sessionEngine.exerciseDone()
    fun finishWorkout() = sessionEngine.finishWorkout()

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
