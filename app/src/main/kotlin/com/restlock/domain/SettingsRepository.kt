package com.restlock.domain

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Persistent user settings. Backed by DataStore in the real implementation.
 */
interface SettingsRepository {
    val chosenRest: Flow<Duration>
    val allowedPackages: Flow<Set<String>>

    @Deprecated(
        message = "The app now uses an allowlist. Read allowedPackages instead.",
        replaceWith = ReplaceWith("allowedPackages"),
    )
    val blockedPackages: Flow<Set<String>>
        get() = allowedPackages

    suspend fun setChosenRest(duration: Duration)
    suspend fun setAllowedPackages(packages: Set<String>)

    @Deprecated(
        message = "The app now uses an allowlist. Call setAllowedPackages instead.",
        replaceWith = ReplaceWith("setAllowedPackages(packages)"),
    )
    suspend fun setBlockedPackages(packages: Set<String>) = setAllowedPackages(packages)

    companion object {
        val DefaultRest: Duration = 90.seconds

        /** Preset rest durations exposed in the setup sheet. */
        val RestPresets: List<Duration> = listOf(
            30.seconds,
            60.seconds,
            90.seconds,
            120.seconds,
            180.seconds,
            240.seconds,
        )
    }
}
