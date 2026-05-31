package com.restlock.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * "Midnight Ember" palette — warm charcoal backgrounds with ember-orange
 * primary and soft-teal secondary. Designed for readability in dim gyms
 * and a premium, energetic fitness feel.
 */
object RestLockPalette {
    // Backdrops (warm charcoal, not navy)
    val Ink0 = Color(0xFF111216)
    val Ink1 = Color(0xFF1A1B21)
    val Ink2 = Color(0xFF222329)
    val Ink3 = Color(0xFF2A2B33)

    // Surfaces (glass with warm undertone)
    val Glass = Color(0x33FFFFFF)
    val GlassStrong = Color(0x4DFFFFFF)
    val GlassOutline = Color(0x28FFFFFF)

    // Text (neutral, not lavender-tinted)
    val TextHigh = Color(0xFFF0F0F5)
    val TextMid = Color(0xFF9CA3AF)
    val TextLow = Color(0xFF6B7280)

    // Brand (ember orange)
    val Ember = Color(0xFFFF6B35)
    val EmberDeep = Color(0xFFE04D1A)

    // Legacy aliases — keep the old property names so screens compile
    // unchanged. They now point at the new brand colour.
    val Violet = Ember
    val VioletDeep = EmberDeep

    // Accents
    val Mint = Color(0xFF2DD4A8)        // soft teal "success"
    val Amber = Color(0xFFFFB547)       // golden amber "warning"
    val Coral = Color(0xFFFF5C72)       // warm rose "danger"
}

val BackdropBrush = Brush.verticalGradient(
    0f to RestLockPalette.Ink0,
    0.55f to RestLockPalette.Ink1,
    1f to RestLockPalette.Ink2,
)

val PrimaryBrush = Brush.linearGradient(
    listOf(RestLockPalette.Ember, RestLockPalette.EmberDeep),
)

val MintBrush = Brush.linearGradient(
    listOf(Color(0xFF2DD4A8), Color(0xFF1FA882)),
)
