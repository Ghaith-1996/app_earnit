package com.fitness.restlock.backend.permissions

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import com.fitness.restlock.backend.PermissionStatus
import com.fitness.restlock.backend.blocking.AppBlockerAccessibilityService

class AndroidPermissionGateway(
    private val context: Context,
) : PermissionGateway {
    private val appContext = context.applicationContext

    override fun currentStatus(): PermissionStatus {
        val canScheduleExact = canScheduleExactAlarms()
        return PermissionStatus(
            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(),
            canScheduleExactAlarms = canScheduleExact,
            needsExactAlarmPermission = false,
        )
    }

    override fun accessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    override fun appDetailsSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${appContext.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return appContext.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(
            appContext,
            AppBlockerAccessibilityService::class.java,
        ).flattenToString()

        val enabled = Settings.Secure.getInt(
            appContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0,
        )
        if (enabled != 1) return false

        val enabledServices = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        for (service in splitter) {
            if (service.equals(expected, ignoreCase = true)) return true
        }
        return false
    }
}
