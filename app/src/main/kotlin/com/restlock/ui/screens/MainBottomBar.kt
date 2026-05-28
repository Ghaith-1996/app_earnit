package com.restlock.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.restlock.ui.theme.RestLockPalette

enum class MainTab {
    Home,
    Workouts,
    Settings,
}

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onHome: () -> Unit,
    onWorkouts: () -> Unit,
    onSettings: () -> Unit,
) {
    NavigationBar(
        containerColor = RestLockPalette.Ink1.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
    ) {
        NavigationBarItem(
            selected = selectedTab == MainTab.Home,
            onClick = onHome,
            icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
            label = { Text("Home") },
            colors = navColors(),
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.Workouts,
            onClick = onWorkouts,
            icon = { Icon(Icons.Rounded.FitnessCenter, contentDescription = null) },
            label = { Text("Workouts") },
            colors = navColors(),
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.Settings,
            onClick = onSettings,
            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
            label = { Text("Settings") },
            colors = navColors(),
        )
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = RestLockPalette.Ink0,
    selectedTextColor = RestLockPalette.TextHigh,
    indicatorColor = RestLockPalette.Mint,
    unselectedIconColor = RestLockPalette.TextLow,
    unselectedTextColor = RestLockPalette.TextLow,
    disabledIconColor = Color.Transparent,
    disabledTextColor = Color.Transparent,
)
