package com.restlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.restlock.domain.InstalledAppsProvider
import com.restlock.domain.SessionEngine
import com.restlock.domain.SettingsRepository
import com.fitness.restlock.backend.permissions.PermissionGateway

class RestLockViewModelFactory(
    private val sessionEngine: SessionEngine,
    private val settingsRepository: SettingsRepository,
    private val installedAppsProvider: InstalledAppsProvider,
    private val permissionGateway: PermissionGateway,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(sessionEngine, settingsRepository, permissionGateway) as T

        modelClass.isAssignableFrom(AppPickerViewModel::class.java) ->
            AppPickerViewModel(installedAppsProvider, settingsRepository) as T

        modelClass.isAssignableFrom(BlockerViewModel::class.java) ->
            BlockerViewModel(sessionEngine) as T

        else -> error("Unknown ViewModel class ${modelClass.name}")
    }
}
