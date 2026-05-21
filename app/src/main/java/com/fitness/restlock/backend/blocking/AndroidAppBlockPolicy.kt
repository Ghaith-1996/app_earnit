package com.fitness.restlock.backend.blocking

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager

class AndroidAppBlockPolicy(context: Context) : AppBlockPolicy {
    private val appContext = context.applicationContext

    override fun isPackageExempt(packageName: String): Boolean {
        if (packageName.isBlank()) return true
        if (packageName == appContext.packageName) return true
        if (packageName in KnownExemptPackages.coreSystemPackages) return true
        if (packageName in KnownExemptPackages.commonLauncherPackages) return true
        if (packageName in KnownExemptPackages.commonHomeSurfacePackages) return true
        if (packageName in launcherPackages()) return true
        if (packageName == defaultDialerPackage()) return true
        return false
    }

    private fun launcherPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val defaultHomePackage = resolveDefaultHomePackage(intent)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.packageManager.queryIntentActivities(
                intent,
                android.content.pm.PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            appContext.packageManager.queryIntentActivities(intent, 0)
        }
        return resolveInfos
            .mapNotNull { it.activityInfo?.packageName }
            .toSet() + listOfNotNull(defaultHomePackage)
    }

    private fun resolveDefaultHomePackage(intent: Intent): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            appContext.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }?.activityInfo?.packageName
    }

    private fun defaultDialerPackage(): String? {
        return runCatching {
            appContext.getSystemService(TelecomManager::class.java).defaultDialerPackage
        }.getOrNull()
    }
}
