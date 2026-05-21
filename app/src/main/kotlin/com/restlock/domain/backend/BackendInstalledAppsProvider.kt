package com.restlock.domain.backend

import com.fitness.restlock.backend.apps.InstalledAppRepository
import com.restlock.domain.InstalledApp
import com.restlock.domain.InstalledAppsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackendInstalledAppsProvider(
    private val installedAppRepository: InstalledAppRepository,
) : InstalledAppsProvider {
    override suspend fun listLaunchableApps(): List<InstalledApp> {
        return withContext(Dispatchers.IO) {
            installedAppRepository.loadInstalledApps()
                .map { app ->
                    InstalledApp(
                        packageName = app.packageName,
                        label = app.label,
                        iconResId = null,
                    )
                }
        }
    }
}
