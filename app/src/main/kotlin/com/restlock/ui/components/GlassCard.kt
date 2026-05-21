package com.restlock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.restlock.ui.theme.RestLockPalette

/**
 * Translucent surface used throughout the app. Approximates glassmorphism on
 * Android (we don't have backdrop-blur as a free primitive, but layered
 * transparency + a soft outline reads as glass against the gradient backdrop).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 28,
    contentPadding: Int = 20,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = RestLockPalette.TextHigh,
        ),
        border = BorderStroke(1.dp, RestLockPalette.GlassOutline),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = 0.08f),
                        1f to Color.White.copy(alpha = 0.02f),
                    )
                )
                .padding(contentPadding.dp),
        ) {
            content()
        }
    }
}
