package com.restlock

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.restlock.ads.CreatorSupportRewardedAd
import com.restlock.domain.SessionState
import com.restlock.ui.BlockerViewModel
import com.restlock.ui.BlockerCompletion
import com.restlock.ui.RestLockViewModelFactory
import com.restlock.ui.screens.BlockerScreen
import com.restlock.ui.screens.SupportCreatorDialog
import com.restlock.ui.theme.RestLockTheme

/**
 * Full-screen lock prompt shown when the user opens a blocked app during
 * [SessionState.Phase.AwaitingDecision].
 *
 * The real backend launches this activity from its AccessibilityService.
 * Rest choices dismiss immediately. Finishing waits for the saved result before
 * handing off to Home, where the persisted summary is displayed.
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
            fitnessRepository = container.fitnessRepository,
            installedAppsProvider = container.installedAppsProvider,
            permissionGateway = container.permissionGateway,
        )

        val blockedAppLabel = intent?.getStringExtra(EXTRA_BLOCKED_APP_LABEL)

        setContent {
            RestLockTheme {
                val vm: BlockerViewModel = viewModel(factory = factory)
                val state by vm.sessionState.collectAsState()
                val preview by vm.activeExercisePreview.collectAsState()
                val completion by vm.completion.collectAsState()
                val pendingSummary by vm.pendingWorkoutSummary.collectAsState()
                var supportDialogOpen by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(state.phase, completion, pendingSummary) {
                    val result = completion
                    when {
                        state.phase == SessionState.Phase.Resting -> finish()
                        state.phase == SessionState.Phase.Idle && pendingSummary != null -> openWorkoutSummary()
                        result is BlockerCompletion.Finished -> {
                            if (result.log != null) openWorkoutSummary() else supportDialogOpen = true
                        }
                        result == BlockerCompletion.Saving -> Unit
                        state.phase != SessionState.Phase.AwaitingDecision -> finish()
                    }
                }

                if (state.phase == SessionState.Phase.AwaitingDecision) {
                    BlockerScreen(
                        setsCompleted = state.setsCompleted,
                        extraRests = state.extraRests,
                        blockedAppLabel = blockedAppLabel,
                        isFinalSet = state.isFinalSet,
                        preview = preview,
                        onExerciseDone = {
                            vm.exerciseDone()
                        },
                        onAddThirtySeconds = {
                            vm.addThirtySeconds()
                        },
                        onFinishWorkout = {
                            vm.finishWorkout()
                        },
                    )
                }

                // Quick start has no structured log. Its session is already over,
                // and declining, dismissing, or failing the ad always allows exit.
                if (supportDialogOpen) {
                    SupportCreatorDialog(
                        onWatchAd = {
                            supportDialogOpen = false
                            CreatorSupportRewardedAd.showOrContinue(this@BlockerActivity) { finish() }
                        },
                        onNoThanks = {
                            supportDialogOpen = false
                            finish()
                        },
                    )
                }
            }
        }
    }

    private fun openWorkoutSummary() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
        finish()
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
