package com.restlock.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Curated palette. Dark-first, slightly violet, with a saturated mint accent
 * to signal "go". No browser-default reds or blues.
 */
object RestLockPalette {
    // Backdrops
    val Ink0 = Color(0xFF07091A)
    val Ink1 = Color(0xFF0B0F1F)
    val Ink2 = Color(0xFF131732)
    val Ink3 = Color(0xFF1B2042)

    // Surfaces (glass)
    val Glass = Color(0x33FFFFFF)
    val GlassStrong = Color(0x4DFFFFFF)
    val GlassOutline = Color(0x1FFFFFFF)

    // Text
    val TextHigh = Color(0xFFE9ECFF)
    val TextMid = Color(0xFFB6BBE0)
    val TextLow = Color(0xFF7C82A8)

    // Brand
    val Violet = Color(0xFF7C5CFF)
    val VioletDeep = Color(0xFF4B30C7)

    // Accents
    val Mint = Color(0xFF4DE3B1)
    val Amber = Color(0xFFFFC15A)
    val Coral = Color(0xFFFF6B81)
}

val BackdropBrush = Brush.verticalGradient(
    0f to RestLockPalette.Ink0,
    0.55f to RestLockPalette.Ink1,
    1f to RestLockPalette.Ink2,
)

val PrimaryBrush = Brush.linearGradient(
    listOf(RestLockPalette.Violet, RestLockPalette.VioletDeep),
)

val MintBrush = Brush.linearGradient(
    listOf(Color(0xFF4DE3B1), Color(0xFF2BAE85)),
)
