package com.restlock.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAlarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.restlock.ads.CreatorSupportRewardedAd
import com.restlock.domain.SessionState
import com.restlock.ui.HomePermissionState
import com.restlock.ui.HomeUiState
import com.restlock.ui.HomeViewModel
import com.restlock.ui.components.GlassCard
import com.restlock.ui.components.PrimaryAction
import com.restlock.ui.components.SecondaryAction
import com.restlock.ui.components.StateChip
import com.restlock.ui.components.TimerRing
import com.restlock.ui.components.toClockString
import com.restlock.ui.components.toCompactLabel
import com.restlock.ui.theme.MintBrush
import com.restlock.ui.theme.PrimaryBrush
import com.restlock.ui.theme.RestLockPalette

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenAppPicker: () -> Unit,
    onOpenPermissions: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var setupOpen by remember { mutableStateOf(false) }
    var supportDialogOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            TopBar(state = state)

            if (state.permissions.needsAction) {
                PermissionBanner(
                    permissions = state.permissions,
                    onOpenPermissions = onOpenPermissions,
                )
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                HeroTimer(state = state)
            }

            ActionDock(
                state = state,
                onStart = { setupOpen = true },
                onAddThirty = viewModel::addThirtySeconds,
                onExerciseDone = viewModel::exerciseDone,
                onFinish = { supportDialogOpen = true },
                onOpenAppPicker = onOpenAppPicker,
            )

            Spacer(Modifier.height(16.dp))
        }

        if (setupOpen) {
            SetupSheet(
                initialRest = state.chosenRest,
                allowedCount = state.allowedAppCount,
                onDismiss = { setupOpen = false },
                onOpenAppPicker = {
                    setupOpen = false
                    onOpenAppPicker()
                },
                onStart = { duration ->
                    setupOpen = false
                    viewModel.startWorkout(duration)
                },
            )
        }

        if (supportDialogOpen) {
            SupportCreatorDialog(
                onWatchAd = {
                    supportDialogOpen = false
                    val activity = context.findActivity()
                    if (activity == null) {
                        viewModel.finishWorkout()
                    } else {
                        CreatorSupportRewardedAd.showOrContinue(activity) {
                            viewModel.finishWorkout()
                        }
                    }
                },
                onNoThanks = {
                    supportDialogOpen = false
                    viewModel.finishWorkout()
                },
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun TopBar(state: HomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "Rest Lock",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
            )
            Text(
                text = "Earn your scroll.",
                style = MaterialTheme.typography.bodyMedium,
                color = RestLockPalette.TextLow,
            )
        }
        StateChip(phase = state.session.phase)
    }
}

@Composable
private fun PermissionBanner(
    permissions: HomePermissionState,
    onOpenPermissions: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "App locking needs setup",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RestLockPalette.TextHigh,
                )
                Text(
                    text = "Review the Accessibility disclosure before enabling app locking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextLow,
                )
            }
            SecondaryAction(
                label = if (permissions.isAccessibilityServiceEnabled) "Review permissions" else "Set up permissions",
                onClick = onOpenPermissions,
                leadingIcon = Icons.Rounded.Tune,
            )
        }
    }
}

@Composable
private fun HeroTimer(state: HomeUiState) {
    val session = state.session
    val (timeText, caption, progress) = when (session.phase) {
        SessionState.Phase.Idle -> Triple(
            state.chosenRest.toClockString(),
            "ready when you are",
            null,
        )
        SessionState.Phase.Resting -> {
            val remaining = session.remaining ?: state.chosenRest
            val totalMs = session.chosenRest.inWholeMilliseconds.coerceAtLeast(1)
            val prog = (remaining.inWholeMilliseconds.toFloat() / totalMs)
            Triple(remaining.toClockString(), "rest until next set", prog)
        }
        SessionState.Phase.AwaitingDecision -> Triple(
            "0:00",
            "pick your next move",
            0f,
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        TimerRing(
            progress = progress,
            timeText = timeText,
            captionText = caption,
        )

        if (session.isInSession) {
            SessionStats(setsCompleted = session.setsCompleted, extraRests = session.extraRests)
        } else {
            IdleHints(allowedAppCount = state.allowedAppCount)
        }

        BlockingDebugStatus(state = state)
    }
}

@Composable
private fun SessionStats(setsCompleted: Int, extraRests: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            label = "Sets",
            value = setsCompleted.toString(),
            tint = RestLockPalette.Mint,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = "+30s used",
            value = extraRests.toString(),
            tint = RestLockPalette.Amber,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, contentPadding = 16) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = RestLockPalette.TextLow,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = tint,
            )
        }
    }
}

@Composable
private fun IdleHints(allowedAppCount: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = null,
                tint = RestLockPalette.TextMid,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (allowedAppCount == 0) "Strict mode is ready"
                    else "$allowedAppCount app${if (allowedAppCount == 1) "" else "s"} allowed",
                    style = MaterialTheme.typography.titleMedium,
                    color = RestLockPalette.TextHigh,
                )
                Text(
                    text = "Everything else locks once your rest timer expires.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextLow,
                )
            }
        }
    }
}

@Composable
private fun BlockingDebugStatus(state: HomeUiState) {
    val diagnostics = state.blockingDiagnostics
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Blocking debug",
                style = MaterialTheme.typography.labelMedium,
                color = RestLockPalette.TextLow,
            )
            Text(
                text = "Accessibility: ${if (state.permissions.isAccessibilityServiceEnabled) "on" else "off"}",
                style = MaterialTheme.typography.bodySmall,
                color = RestLockPalette.TextMid,
            )
            Text(
                text = "Lock mode: ${if (diagnostics.lockActive) "active" else "inactive"}",
                style = MaterialTheme.typography.bodySmall,
                color = RestLockPalette.TextMid,
            )
            Text(
                text = "Detected: ${diagnostics.lastDetectedPackage ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = RestLockPalette.TextMid,
            )
            Text(
                text = "Blocked: ${diagnostics.lastBlockedPackage ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = RestLockPalette.TextMid,
            )
        }
    }
}

@Composable
private fun ActionDock(
    state: HomeUiState,
    onStart: () -> Unit,
    onAddThirty: () -> Unit,
    onExerciseDone: () -> Unit,
    onFinish: () -> Unit,
    onOpenAppPicker: () -> Unit,
) {
    AnimatedContent(
        targetState = state.session.phase,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "actionDock",
    ) { phase ->
        when (phase) {
            SessionState.Phase.Idle -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryAction(
                    label = "Start workout",
                    onClick = onStart,
                    leadingIcon = Icons.Rounded.PlayArrow,
                    brush = PrimaryBrush,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryAction(
                        label = "Allowed apps",
                        onClick = onOpenAppPicker,
                        leadingIcon = Icons.Rounded.Tune,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryAction(
                        label = state.chosenRest.toCompactLabel(),
                        onClick = onStart,
                        leadingIcon = Icons.Rounded.AddAlarm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SessionState.Phase.Resting -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Resting - non-allowed apps will lock when the timer ends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextLow,
                )
                SecondaryAction(
                    label = "Finish workout",
                    onClick = onFinish,
                    leadingIcon = Icons.Rounded.Stop,
                )
            }

            SessionState.Phase.AwaitingDecision -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryAction(
                    label = "Exercise done",
                    onClick = onExerciseDone,
                    leadingIcon = Icons.Rounded.CheckCircle,
                    brush = MintBrush,
                    contentColor = RestLockPalette.Ink0,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryAction(
                        label = "+30s rest",
                        onClick = onAddThirty,
                        leadingIcon = Icons.Rounded.AddAlarm,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryAction(
                        label = "Finish",
                        onClick = onFinish,
                        leadingIcon = Icons.Rounded.Stop,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
