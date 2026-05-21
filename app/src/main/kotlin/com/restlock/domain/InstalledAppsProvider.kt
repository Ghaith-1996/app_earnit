package com.restlock.domain

/**
 * Lists the installed apps the user is allowed to choose from in the picker.
 *
 * The real implementation queries the PackageManager for activities with
 * [Intent.ACTION_MAIN] / [Intent.CATEGORY_LAUNCHER] (no QUERY_ALL_PACKAGES),
 * and filters out the host app itself, the active launcher, settings, dialer,
 * emergency, and core system packages.
 */
interface InstalledAppsProvider {
    suspend fun listLaunchableApps(): List<InstalledApp>
}

data class InstalledApp(
    val packageName: String,
    val label: String,
    /** Resource id of an icon, or null when we render an initial placeholder. */
    val iconResId: Int? = null,
)
