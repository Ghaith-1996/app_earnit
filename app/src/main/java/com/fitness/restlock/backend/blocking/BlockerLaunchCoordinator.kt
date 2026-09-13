package com.fitness.restlock.backend.blocking

import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Main-thread coordinator; Android supplies package events, a monotonic clock and actions. */
internal class BlockerLaunchCoordinator(
    private val policy: AppBlockPolicy,
    private val scope: CoroutineScope,
    private val currentState: () -> WorkoutState,
    private val elapsedRealtime: () -> Long,
    private val goHome: () -> Unit,
    private val openBlocker: (String) -> Boolean,
) {
    private var foregroundPackage: String? = null
    private var pendingPackage: String? = null
    private var pendingLaunch: Job? = null
    private var lastLaunchMillis: Long? = null
    private var launchGeneration = 0L
    private var throttleRetry: Job? = null
    private var retryGeneration = 0L

    fun onForegroundPackageChanged(packageName: String?) {
        // Retain only the latest package during a workout so expiry also handles an
        // app that was already open during rest. Never retain packages while idle.
        foregroundPackage = if (currentState().mode == WorkoutMode.Idle) null
            else packageName?.trim()?.takeIf { it.isNotEmpty() }
        onStateChanged()
    }

    fun onStateChanged() {
        val state = currentState()
        if (state.mode != WorkoutMode.AwaitingDecision) {
            cancelPendingLaunch()
            cancelThrottleRetry()
            lastLaunchMillis = null
            if (state.mode == WorkoutMode.Idle) foregroundPackage = null
            return
        }
        if (pendingPackage != null && !shouldBlock(pendingPackage, state)) {
            cancelPendingLaunch()
            lastLaunchMillis = null
        }
        val blockedPackage = foregroundPackage
        if (blockedPackage == null || !shouldBlock(blockedPackage, state)) {
            cancelThrottleRetry()
            return
        }
        if (pendingLaunch != null) return
        val now = elapsedRealtime()
        val cooldownRemaining = lastLaunchMillis?.let { LAUNCH_THROTTLE_MILLIS - (now - it) } ?: 0L
        if (cooldownRemaining > 0L) {
            if (throttleRetry == null) {
                val generation = ++retryGeneration
                throttleRetry = scope.launch {
                    delay(cooldownRemaining)
                    if (generation == retryGeneration) {
                        throttleRetry = null
                        onStateChanged()
                    }
                }
            }
            return
        }
        cancelThrottleRetry()

        // A global throttle also coalesces bursts that alternate between packages.
        lastLaunchMillis = now
        foregroundPackage = null
        pendingPackage = blockedPackage
        val generation = ++launchGeneration
        goHome()
        pendingLaunch = scope.launch {
            try {
                delay(BLOCKER_LAUNCH_DELAY_MILLIS)
                // The user may have finished, added rest, or allowed this app after HOME.
                if (shouldBlock(blockedPackage, currentState()) && !openBlocker(blockedPackage)) {
                    lastLaunchMillis = null
                }
            } finally {
                if (generation == launchGeneration) {
                    pendingPackage = null
                    pendingLaunch = null
                }
            }
        }
    }

    fun reset() {
        cancelPendingLaunch()
        cancelThrottleRetry()
        foregroundPackage = null
        lastLaunchMillis = null
    }

    private fun shouldBlock(packageName: String?, state: WorkoutState): Boolean =
        state.mode == WorkoutMode.AwaitingDecision &&
            policy.shouldBlock(packageName, state.allowedApps, lockActive = true)

    private fun cancelPendingLaunch() {
        launchGeneration++
        pendingLaunch?.cancel()
        pendingLaunch = null
        pendingPackage = null
    }

    private fun cancelThrottleRetry() {
        retryGeneration++
        throttleRetry?.cancel()
        throttleRetry = null
    }

    private companion object {
        const val LAUNCH_THROTTLE_MILLIS = 1_000L
        const val BLOCKER_LAUNCH_DELAY_MILLIS = 120L
    }
}
