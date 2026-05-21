package com.fitness.restlock.backend.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.fitness.restlock.backend.blocking.AppBlockPolicy
import java.util.Locale

class AndroidInstalledAppRepository(
    context: Context,
    private val blockPolicy: AppBlockPolicy,
) : InstalledAppRepository {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    override fun loadInstalledApps(): List<InstalledApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return queryLauncherActivities(launcherIntent)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val packageName = activityInfo.packageName ?: return@mapNotNull null
                if (blockPolicy.isPackageExempt(packageName)) return@mapNotNull null
                InstalledApp(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(packageManager)?.toString().orEmpty()
                        .ifBlank { packageName },
                    icon = resolveInfo.loadIcon(packageManager),
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ it.label.lowercase(Locale.getDefault()) }, { it.packageName }))
            .toList()
    }

    private fun queryLauncherActivities(intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
    }
}
