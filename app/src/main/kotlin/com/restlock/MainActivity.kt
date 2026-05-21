package com.restlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.restlock.ui.RestLockViewModelFactory
import com.restlock.ui.nav.RestLockNavGraph
import com.restlock.ui.nav.Routes
import com.restlock.ui.theme.RestLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = application as RestLockApp
        val factory = RestLockViewModelFactory(
            sessionEngine = container.sessionEngine,
            settingsRepository = container.settingsRepository,
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
}
