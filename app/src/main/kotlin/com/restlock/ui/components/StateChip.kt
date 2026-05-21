package com.restlock.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.restlock.domain.SessionState
import com.restlock.ui.theme.RestLockPalette

private data class ChipAppearance(
    val label: String,
    val color: Color,
    val pulses: Boolean,
)

@Composable
fun StateChip(
    phase: SessionState.Phase,
    modifier: Modifier = Modifier,
) {
    val appearance = when (phase) {
        SessionState.Phase.Idle -> ChipAppearance(
            label = "READY",
            color = RestLockPalette.TextLow,
            pulses = false,
        )
        SessionState.Phase.Resting -> ChipAppearance(
            label = "RESTING",
            color = RestLockPalette.Mint,
            pulses = true,
        )
        SessionState.Phase.AwaitingDecision -> ChipAppearance(
            label = "DECISION",
            color = RestLockPalette.Amber,
            pulses = true,
        )
    }

    val pulseAlpha = if (appearance.pulses) {
        rememberInfiniteTransition(label = "chipPulse").animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "chipPulseAlpha",
        ).value
    } else 1f

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(8.dp)
                .drawBehind {
                    drawCircle(color = appearance.color.copy(alpha = pulseAlpha))
                },
        )
        Text(
            text = appearance.label,
            style = MaterialTheme.typography.labelMedium,
            color = appearance.color,
        )
    }
}
