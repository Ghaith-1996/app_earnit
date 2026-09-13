package com.restlock

import com.fitness.restlock.backend.RestLockApplication
import com.fitness.restlock.backend.RestLockBackend
import com.restlock.ads.CreatorSupportRewardedAd
import com.restlock.domain.SessionEngine
import com.restlock.domain.FitnessRepository
import com.restlock.domain.SettingsRepository
import com.restlock.domain.InstalledAppsProvider
import com.restlock.domain.backend.BackendFitnessRepository
import com.restlock.domain.backend.BackendInstalledAppsProvider
import com.restlock.domain.backend.BackendSessionEngine
import com.restlock.domain.backend.BackendSettingsRepository
import com.fitness.restlock.backend.permissions.PermissionGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application container.
 *
 * The frontend depends only on interfaces in [com.restlock.domain].
 * Production wiring points those interfaces at the Android local backend:
 * DataStore, PackageManager, AlarmManager, and AccessibilityService.
 */
class RestLockApp : RestLockApplication() {

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var sessionEngine: SessionEngine
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var fitnessRepository: FitnessRepository
        private set

    lateinit var installedAppsProvider: InstalledAppsProvider
        private set

    lateinit var permissionGateway: PermissionGateway
        private set

    override fun onCreate() {
        super.onCreate()
        val controller = RestLockBackend.controller(this)
        permissionGateway = RestLockBackend.permissions(this)
        settingsRepository = BackendSettingsRepository(controller)
        fitnessRepository = BackendFitnessRepository(this)
        installedAppsProvider = BackendInstalledAppsProvider(
            RestLockBackend.installedApps(this),
        )
        sessionEngine = BackendSessionEngine(controller, fitnessRepository, appScope)
        CreatorSupportRewardedAd.initialize(this)
    }
}
