package com.restlock.ui.components

import com.fitness.restlock.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable

import kotlin.math.ceil
import kotlin.time.Duration

/**
 * Format a duration as M:SS for the timer display.
 *
 * We round up so the user never sees "0:00" while the timer is still ticking;
 * the ring turns over to AwaitingDecision when the engine emits expiry.
 */
fun Duration.toClockString(): String {
    val totalSeconds = ceil(this.inWholeMilliseconds / 1000.0).toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/**
 * Short, human label like "90s" or "3m" for chips and presets.
 */
@Composable
fun Duration.toCompactLabel(): String {
    val secs = this.inWholeSeconds
    return when {
        secs < 60 -> stringResource(R.string.duration_compact_seconds, secs)
        secs % 60 == 0L -> stringResource(R.string.duration_compact_minutes, secs / 60)
        else -> stringResource(R.string.duration_compact_minutes_seconds, secs / 60, secs % 60)
    }
}
