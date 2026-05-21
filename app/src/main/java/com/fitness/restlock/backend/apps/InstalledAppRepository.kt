package com.fitness.restlock.backend.apps

interface InstalledAppRepository {
    fun loadInstalledApps(): List<InstalledApp>
}
