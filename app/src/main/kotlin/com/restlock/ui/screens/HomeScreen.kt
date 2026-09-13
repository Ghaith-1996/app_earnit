package com.restlock.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAlarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.restlock.ads.CreatorSupportRewardedAd
import com.restlock.domain.ActiveExercisePreview
import com.restlock.domain.ExerciseCatalog
import com.restlock.domain.FitnessCalculator
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.SessionState
import com.restlock.domain.UserProfile
import com.restlock.domain.WorkoutLog
import com.restlock.ui.HomePermissionState
import com.restlock.ui.HomeUiState
import com.restlock.ui.HomeViewModel
import com.restlock.ui.components.ExerciseArtwork
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenAppPicker: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenWorkout: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val pendingSummary by viewModel.pendingWorkoutSummary.collectAsState()
    val context = LocalContext.current
    var setupOpen by remember { mutableStateOf(false) }
    var workoutChooserOpen by remember { mutableStateOf(false) }
    var supportDialogOpen by rememberSaveable { mutableStateOf(false) }
    val openRestSetup = { setupOpen = true }
    val openWorkoutLaunch = {
        if (state.savedWorkouts.isEmpty()) {
            setupOpen = true
        } else {
            workoutChooserOpen = true
        }
    }

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.permissions.needsAction) {
                    PermissionBanner(
                        permissions = state.permissions,
                        onOpenPermissions = onOpenPermissions,
                    )
                }

                if (state.session.phase == SessionState.Phase.Idle) {
                    QuickStartCard(
                        state = state,
                        onQuickStart = openRestSetup,
                        onStartSavedWorkout = openWorkoutLaunch,
                        onOpenRestSetup = openRestSetup,
                        onOpenWorkout = onOpenWorkout,
                        onOpenAppPicker = onOpenAppPicker,
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    HeroTimer(state = state)
                }

                RecentWorkoutsCard(logs = state.recentWorkoutLogs)
                if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    BlockingDebugStatus(state = state)
                }
            }

            if (state.session.phase != SessionState.Phase.Idle) {
                Spacer(Modifier.height(12.dp))
                ActionDock(
                    state = state,
                    onStart = openWorkoutLaunch,
                    onAddThirty = viewModel::addThirtySeconds,
                    onExerciseDone = viewModel::exerciseDone,
                    onFinish = {
                        viewModel.finishWorkout { log ->
                            if (log == null) supportDialogOpen = true
                        }
                    },
                    onOpenAppPicker = onOpenAppPicker,
                )
            }

            Spacer(Modifier.height(12.dp))
            MainBottomBar(
                selectedTab = MainTab.Home,
                onHome = {},
                onWorkouts = onOpenWorkout,
                onSettings = onOpenSettings,
            )
            Spacer(Modifier.height(8.dp))
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

        if (workoutChooserOpen) {
            WorkoutChoiceSheet(
                workouts = state.savedWorkouts,
                profile = state.userProfile,
                restLabel = state.chosenRest.toCompactLabel(),
                onDismiss = { workoutChooserOpen = false },
                onChooseWorkout = { workout ->
                    workoutChooserOpen = false
                    viewModel.startSavedWorkout(workout.id, state.chosenRest)
                },
            )
        }

        pendingSummary?.let { summary ->
            WorkoutSummaryDialog(
                log = summary,
                onDismiss = viewModel::dismissWorkoutSummary,
                onSupportCreator = {
                    viewModel.dismissWorkoutSummary()
                    supportDialogOpen = true
                },
            )
        }

        if (supportDialogOpen && pendingSummary == null) {
            SupportCreatorDialog(
                onWatchAd = {
                    supportDialogOpen = false
                    val activity = context.findActivity()
                    if (activity != null) {
                        CreatorSupportRewardedAd.showOrContinue(activity) {}
                    }
                },
                onNoThanks = {
                    supportDialogOpen = false
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
                text = "Earn it!",
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
private fun QuickStartCard(
    state: HomeUiState,
    onQuickStart: () -> Unit,
    onStartSavedWorkout: () -> Unit,
    onOpenRestSetup: () -> Unit,
    onOpenWorkout: () -> Unit,
    onOpenAppPicker: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Quick start",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = RestLockPalette.TextHigh,
                )
                Text(
                    text = "Start a ${state.chosenRest.toCompactLabel()} rest lock with ${state.allowedAppCount} allowed app${if (state.allowedAppCount == 1) "" else "s"}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextLow,
                )
            }
            PrimaryAction(
                label = "Quick start",
                onClick = onQuickStart,
                leadingIcon = Icons.Rounded.PlayArrow,
                brush = PrimaryBrush,
            )
            if (state.savedWorkouts.isNotEmpty()) {
                SecondaryAction(
                    label = "Start saved workout",
                    onClick = onStartSavedWorkout,
                    leadingIcon = Icons.Rounded.PlayArrow,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryAction(
                    label = state.chosenRest.toCompactLabel(),
                    onClick = onOpenRestSetup,
                    leadingIcon = Icons.Rounded.AddAlarm,
                    modifier = Modifier.weight(1f),
                )
                SecondaryAction(
                    label = "Allowed apps",
                    onClick = onOpenAppPicker,
                    leadingIcon = Icons.Rounded.Tune,
                    modifier = Modifier.weight(1f),
                )
            }
            SecondaryAction(
                label = "Build workout",
                onClick = onOpenWorkout,
                leadingIcon = Icons.Rounded.FitnessCenter,
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
            Triple(
                remaining.toClockString(),
                state.activeExercisePreview?.definition?.name ?: state.activeWorkout?.name ?: "rest until next set",
                prog,
            )
        }
        SessionState.Phase.AwaitingDecision -> Triple(
            "0:00",
            state.activeExercisePreview?.definition?.name ?: state.activeWorkout?.name ?: "pick your next move",
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
            size = if (session.isInSession) 252.dp else 208.dp,
        )

        if (session.isInSession) {
            ActiveExercisePreviewCard(
                preview = state.activeExercisePreview,
                workout = state.activeWorkout,
            )
            SessionStats(setsCompleted = session.setsCompleted, extraRests = session.extraRests)
        } else {
            IdleHints(allowedAppCount = state.allowedAppCount)
        }
    }
}

@Composable
private fun ActiveExercisePreviewCard(
    preview: ActiveExercisePreview?,
    workout: PlannedWorkout?,
) {
    if (workout == null) return

    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16) {
        if (preview == null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Workout complete",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RestLockPalette.TextHigh,
                )
                Text(
                    text = "All ${workout.totalSets} sets are marked done. Finish the workout when you are ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextLow,
                )
            }
            return@GlassCard
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(86.dp),
                contentAlignment = Alignment.Center,
            ) {
                ExerciseArtwork(
                    exercise = preview.definition,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 18.dp,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Current exercise",
                    style = MaterialTheme.typography.labelMedium,
                    color = RestLockPalette.TextLow,
                )
                Text(
                    text = preview.definition.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RestLockPalette.TextHigh,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Rank ${preview.exerciseRank}/${preview.exerciseCount} - Set ${preview.setNumberForExercise}/${preview.totalSetsForExercise} - ${preview.plannedExercise.reps} reps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextMid,
                )
                Text(
                    text = "${preview.completedSetsInWorkout}/${preview.totalSetsInWorkout} sets completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = RestLockPalette.TextLow,
                )
            }
        }
    }
}

@Composable
private fun RecentWorkoutsCard(logs: List<WorkoutLog>) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Last workouts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = RestLockPalette.TextHigh,
                )
                Text(
                    text = "Latest 3 sessions with approximate calories burned.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextLow,
                )
            }

            if (logs.isEmpty()) {
                Text(
                    text = "No workouts logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextMid,
                )
            } else {
                logs.forEach { log ->
                    RecentWorkoutRow(log = log)
                }
            }
        }
    }
}

@Composable
private fun RecentWorkoutRow(log: WorkoutLog) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
            )
            Text(
                text = "${formatWorkoutDate(log.completedAtMillis)} · ${log.durationLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = RestLockPalette.TextLow,
            )
            Text(
                text = "${log.setsLabel()} · ${log.completionLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = RestLockPalette.TextLow,
            )
        }
        Text(
            text = "~${log.calories} kcal",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = RestLockPalette.Mint,
        )
    }
}

private fun formatWorkoutDate(epochMillis: Long): String {
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMillis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutChoiceSheet(
    workouts: List<PlannedWorkout>,
    profile: UserProfile,
    restLabel: String,
    onDismiss: () -> Unit,
    onChooseWorkout: (PlannedWorkout) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RestLockPalette.Ink2,
        contentColor = RestLockPalette.TextHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .background(
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(2.dp),
                    )
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Choose workout",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = RestLockPalette.TextHigh,
                    )
                    Text(
                        text = "Timer starts with your $restLabel rest interval.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RestLockPalette.TextMid,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = RestLockPalette.TextMid,
                    )
                }
            }

            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                workouts.filter { workout ->
                    workout.exercises.isNotEmpty() &&
                        workout.exercises.all { ExerciseCatalog.byId(it.exerciseId) != null }
                }.forEach { workout ->
                    WorkoutChoiceRow(
                        workout = workout,
                        calories = FitnessCalculator.caloriesForPlannedExercises(workout.exercises, profile),
                        minutes = FitnessCalculator.durationForPlannedExercises(workout.exercises),
                        onClick = { onChooseWorkout(workout) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WorkoutChoiceRow(
    workout: PlannedWorkout,
    calories: Int,
    minutes: Int,
    onClick: () -> Unit,
) {
    val coverExercise = workout.orderedExercises
        .firstOrNull()
        ?.exerciseId
        ?.let(ExerciseCatalog::byId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (coverExercise != null) {
            ExerciseArtwork(exercise = coverExercise, modifier = Modifier.size(48.dp))
        } else {
            Icon(
                imageVector = Icons.Rounded.FitnessCenter,
                contentDescription = null,
                tint = RestLockPalette.Mint,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${workout.exercises.size} exercises - ${workout.totalSets} sets - $minutes min - ~$calories kcal",
                style = MaterialTheme.typography.bodySmall,
                color = RestLockPalette.TextLow,
            )
        }
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = RestLockPalette.TextMid,
        )
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
                    label = if (state.session.isFinalSet) "Finish final set" else "Exercise done",
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
