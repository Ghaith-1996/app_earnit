package com.restlock

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.restlock.ads.CreatorSupportRewardedAd
import com.restlock.domain.SessionState
import com.restlock.ui.BlockerViewModel
import com.restlock.ui.RestLockViewModelFactory
import com.restlock.ui.screens.BlockerScreen
import com.restlock.ui.screens.SupportCreatorDialog
import com.restlock.ui.theme.RestLockTheme

/**
 * Full-screen lock prompt shown when the user opens a blocked app during
 * [SessionState.Phase.AwaitingDecision].
 *
 * The real backend launches this activity from its AccessibilityService.
 * The activity finishes itself as soon as the session leaves AwaitingDecision
 * (the user chose Exercise Done, +30s, or Finish).
 */
class BlockerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureShowWhenLocked()

        val container = application as RestLockApp
        val factory = RestLockViewModelFactory(
            sessionEngine = container.sessionEngine,
            settingsRepository = container.settingsRepository,
            installedAppsProvider = container.installedAppsProvider,
            permissionGateway = container.permissionGateway,
        )

        val blockedAppLabel = intent?.getStringExtra(EXTRA_BLOCKED_APP_LABEL)

        setContent {
            RestLockTheme {
                val vm: BlockerViewModel = viewModel(factory = factory)
                val state by vm.sessionState.collectAsState()
                var supportDialogOpen by remember { mutableStateOf(false) }

                if (state.phase != SessionState.Phase.AwaitingDecision) {
                    // The user resolved the lock elsewhere (or the session ended);
                    // dismiss ourselves so the user is back where they were.
                    finish()
                }

                BlockerScreen(
                    setsCompleted = state.setsCompleted,
                    extraRests = state.extraRests,
                    blockedAppLabel = blockedAppLabel,
                    onExerciseDone = {
                        vm.exerciseDone()
                        finish()
                    },
                    onAddThirtySeconds = {
                        vm.addThirtySeconds()
                        finish()
                    },
                    onFinishWorkout = {
                        supportDialogOpen = true
                    },
                )

                if (supportDialogOpen) {
                    SupportCreatorDialog(
                        onWatchAd = {
                            supportDialogOpen = false
                            CreatorSupportRewardedAd.showOrContinue(this@BlockerActivity) {
                                vm.finishWorkout()
                                finish()
                            }
                        },
                        onNoThanks = {
                            supportDialogOpen = false
                            vm.finishWorkout()
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun configureShowWhenLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    companion object {
        /** Optional human-readable label for the app the user just opened. */
        const val EXTRA_BLOCKED_APP_LABEL: String = "extra_blocked_app_label"
    }
}
