package com.restlock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box

private val RestLockColorScheme = darkColorScheme(
    primary = RestLockPalette.Ember,
    onPrimary = Color.White,
    primaryContainer = RestLockPalette.EmberDeep,
    onPrimaryContainer = Color.White,
    secondary = RestLockPalette.Mint,
    onSecondary = RestLockPalette.Ink0,
    tertiary = RestLockPalette.Amber,
    background = RestLockPalette.Ink0,
    onBackground = RestLockPalette.TextHigh,
    surface = RestLockPalette.Ink1,
    onSurface = RestLockPalette.TextHigh,
    surfaceVariant = RestLockPalette.Ink2,
    onSurfaceVariant = RestLockPalette.TextMid,
    outline = RestLockPalette.GlassOutline,
    error = RestLockPalette.Coral,
    onError = Color.White,
)

@Composable
fun RestLockTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = false
            insets.isAppearanceLightNavigationBars = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    MaterialTheme(
        colorScheme = RestLockColorScheme,
        typography = RestLockTypography,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackdropBrush),
        ) {
            content()
        }
    }
}
