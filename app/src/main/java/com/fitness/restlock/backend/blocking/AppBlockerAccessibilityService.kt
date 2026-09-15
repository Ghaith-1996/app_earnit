package com.fitness.restlock.backend.blocking

import android.accessibilityservice.AccessibilityService
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.fitness.restlock.backend.RestLockBackend
import com.fitness.restlock.backend.WorkoutMode
import com.restlock.BlockerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val policy: AppBlockPolicy by lazy { AndroidAppBlockPolicy(this) }
    private val controller by lazy { RestLockBackend.controller(this) }
    private val debugDiagnostics by lazy { applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }
    private var stateCollection: Job? = null
    private val launchCoordinator by lazy {
        BlockerLaunchCoordinator(
            policy = policy,
            scope = serviceScope,
            currentState = { controller.state.value },
            elapsedRealtime = SystemClock::elapsedRealtime,
            goHome = { performGlobalAction(GLOBAL_ACTION_HOME); Unit },
            openBlocker = { packageName ->
                runCatching { startActivity(blockerIntent(packageName)) }
                    .onSuccess {
                        if (debugDiagnostics) {
                            BlockingDiagnostics.updateLastBlocked(packageName)
                            Log.d(TAG, "blocking package=$packageName")
                        }
                    }
                    .onFailure { if (debugDiagnostics) Log.w(TAG, "Blocker launch failed", it) }
                    .isSuccess
            },
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        stateCollection?.cancel()
        launchCoordinator.reset()
        stateCollection = serviceScope.launch {
            controller.state.collect { state ->
                if (debugDiagnostics) {
                    BlockingDiagnostics.updateLockActive(state.mode == WorkoutMode.AwaitingDecision)
                    Log.d(TAG, "state=${state.mode}, allowed=${state.allowedApps.size}")
                }
                launchCoordinator.onStateChanged()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (controller.state.value.mode == WorkoutMode.Idle) return
        // Package metadata only: no source nodes, text, or window-content APIs.
        val packageName = event.packageName?.toString()
        if (debugDiagnostics && !packageName.isNullOrBlank()) {
            BlockingDiagnostics.updateLastDetected(packageName)
            Log.d(TAG, "detected package=$packageName event=${event.eventType}")
        }
        launchCoordinator.onForegroundPackageChanged(packageName)
    }

    override fun onDestroy() {
        launchCoordinator.reset()
        if (debugDiagnostics) BlockingDiagnostics.reset()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() {
        launchCoordinator.reset()
        if (debugDiagnostics) BlockingDiagnostics.reset()
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
    }
}
