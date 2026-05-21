package com.restlock.ui.components

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
fun Duration.toCompactLabel(): String {
    val secs = this.inWholeSeconds
    return when {
        secs < 60 -> "${secs}s"
        secs % 60 == 0L -> "${secs / 60}m"
        else -> "${secs / 60}m ${secs % 60}s"
    }
}
