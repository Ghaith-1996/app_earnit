package com.fitness.restlock.backend.blocking

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.fitness.restlock.backend.RestLockBackend
import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutState
import com.restlock.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val policy: AppBlockPolicy by lazy { AndroidAppBlockPolicy(this) }
    private var lastLaunchedPackage: String? = null
    private var lastLaunchElapsedMillis: Long = 0L
    private var currentForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            RestLockBackend.controller(this@AppBlockerAccessibilityService)
                .state
                .collectLatest { state ->
                    BlockingDiagnostics.updateLockActive(state.mode == WorkoutMode.AwaitingDecision)
                    Log.d(TAG, "state=${state.mode}, allowed=${state.allowedApps.size}")
                    maybeLaunchBlocker(currentForegroundPackage, state)
                }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        currentForegroundPackage = packageName
        BlockingDiagnostics.updateLastDetected(packageName)
        Log.d(TAG, "detected package=$packageName event=${event.eventType}")
        val state = RestLockBackend.controller(this).state.value
        maybeLaunchBlocker(packageName, state)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    private fun maybeLaunchBlocker(
        packageName: String?,
        state: WorkoutState,
    ) {
        val lockActive = state.mode == WorkoutMode.AwaitingDecision
        if (!policy.shouldBlock(packageName, state.allowedApps, lockActive)) return
        val blockedPackage = packageName ?: return
        if (isLaunchThrottled(blockedPackage)) return

        lastLaunchedPackage = blockedPackage
        lastLaunchElapsedMillis = android.os.SystemClock.elapsedRealtime()
        currentForegroundPackage = null
        BlockingDiagnostics.updateLastBlocked(blockedPackage)
        Log.d(TAG, "blocking package=$blockedPackage")
        performGlobalAction(GLOBAL_ACTION_HOME)
        serviceScope.launch {
            delay(BLOCKER_LAUNCH_DELAY_MILLIS)
            startActivity(blockerIntent(blockedPackage))
        }
    }

    private fun isLaunchThrottled(packageName: String): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        return packageName == lastLaunchedPackage &&
            now - lastLaunchElapsedMillis < LAUNCH_THROTTLE_MILLIS
    }

    private fun blockerIntent(packageName: String): android.content.Intent {
        val label = runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)

        return android.content.Intent(this, BlockerActivity::class.java)
            .putExtra(BlockerActivity.EXTRA_BLOCKED_APP_LABEL, label)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    private companion object {
        const val TAG = "RestLockBlocker"
        const val LAUNCH_THROTTLE_MILLIS = 1_000L
        const val BLOCKER_LAUNCH_DELAY_MILLIS = 120L
    }
}
