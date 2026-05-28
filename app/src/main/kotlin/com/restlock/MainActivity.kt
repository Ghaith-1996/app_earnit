package com.restlock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.restlock.ui.RestLockViewModelFactory
import com.restlock.ui.nav.RestLockNavGraph
import com.restlock.ui.nav.Routes
import com.restlock.ui.theme.RestLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val container = application as RestLockApp
        val factory = RestLockViewModelFactory(
            sessionEngine = container.sessionEngine,
            settingsRepository = container.settingsRepository,
            fitnessRepository = container.fitnessRepository,
            installedAppsProvider = container.installedAppsProvider,
            permissionGateway = container.permissionGateway,
        )
        val startDestination = if (container.permissionGateway.currentStatus().isAccessibilityServiceEnabled) {
            Routes.Home
        } else {
            Routes.Permissions
        }

        setContent {
            RestLockTheme {
                RestLockNavGraph(
                    factory = factory,
                    startDestination = startDestination,
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            permission,
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                NOTIFICATION_PERMISSION_REQUEST_CODE,
            )
        }
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}
