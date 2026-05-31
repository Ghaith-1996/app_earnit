package com.restlock.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.restlock.ui.theme.RestLockPalette

/**
 * The big countdown ring. Animates between progress values and renders the
 * remaining time + optional caption in the center.
 *
 * When [progress] is null the ring shows a soft idle pulse instead.
 */
@Composable
fun TimerRing(
    progress: Float?,
    timeText: String,
    captionText: String?,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    trackBrush: Brush = Brush.sweepGradient(
        listOf(RestLockPalette.Ember, RestLockPalette.Mint, RestLockPalette.Ember),
    ),
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = tween(durationMillis = 350, easing = LinearEasing),
        label = "ringProgress",
    )

    val pulse = if (progress == null) {
        val transition = rememberInfiniteTransition(label = "idlePulse")
        transition.animateFloat(
            initialValue = 0.04f,
            targetValue = 0.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "idlePulseValue",
        ).value
    } else 0f

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = size.toPx() * 0.07f
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Track
            drawArc(
                color = Color.White.copy(alpha = 0.08f + pulse),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Progress
            if (progress != null) {
                drawArc(
                    brush = trackBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum",
                ),
                color = LocalContentColor.current,
                textAlign = TextAlign.Center,
            )
            if (captionText != null) {
                Text(
                    text = captionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = RestLockPalette.TextMid,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
