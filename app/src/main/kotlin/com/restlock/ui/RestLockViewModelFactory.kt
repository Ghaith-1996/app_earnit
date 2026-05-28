package com.restlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.restlock.domain.FitnessRepository
import com.restlock.domain.InstalledAppsProvider
import com.restlock.domain.SessionEngine
import com.restlock.domain.SettingsRepository
import com.fitness.restlock.backend.permissions.PermissionGateway

class RestLockViewModelFactory(
    private val sessionEngine: SessionEngine,
    private val settingsRepository: SettingsRepository,
    private val fitnessRepository: FitnessRepository,
    private val installedAppsProvider: InstalledAppsProvider,
    private val permissionGateway: PermissionGateway,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(sessionEngine, settingsRepository, fitnessRepository, permissionGateway) as T

        modelClass.isAssignableFrom(WorkoutViewModel::class.java) ->
            WorkoutViewModel(fitnessRepository) as T

        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(fitnessRepository) as T

        modelClass.isAssignableFrom(AppPickerViewModel::class.java) ->
            AppPickerViewModel(installedAppsProvider, settingsRepository) as T

        modelClass.isAssignableFrom(BlockerViewModel::class.java) ->
            BlockerViewModel(sessionEngine, fitnessRepository) as T

        else -> error("Unknown ViewModel class ${modelClass.name}")
    }
}
