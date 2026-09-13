package com.restlock.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAlarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.restlock.ui.components.GlassCard
import com.restlock.domain.ActiveExercisePreview
import com.restlock.ui.components.PrimaryAction
import com.restlock.ui.components.SecondaryAction
import com.restlock.ui.theme.BackdropBrush
import com.restlock.ui.theme.MintBrush
import com.restlock.ui.theme.RestLockPalette

/**
 * Full-screen lock prompt shown when the user opens a blocked app during
 * AwaitingDecision. Mirrors the three actions on the home screen so the user
 * can resolve the lock from wherever they happen to be.
 */
@Composable
fun BlockerScreen(
    setsCompleted: Int,
    extraRests: Int,
    blockedAppLabel: String?,
    isFinalSet: Boolean,
    preview: ActiveExercisePreview?,
    onExerciseDone: () -> Unit,
    onAddThirtySeconds: () -> Unit,
    onFinishWorkout: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackdropBrush)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BlockerHeader(blockedAppLabel = blockedAppLabel)

            BlockerHero(preview)

            BlockerActions(
                setsCompleted = setsCompleted,
                extraRests = extraRests,
                isFinalSet = isFinalSet,
                onExerciseDone = onExerciseDone,
                onAddThirtySeconds = onAddThirtySeconds,
                onFinishWorkout = onFinishWorkout,
            )
        }
    }
}

@Composable
private fun BlockerHeader(blockedAppLabel: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "EARN IT!",
            style = MaterialTheme.typography.labelMedium,
            color = RestLockPalette.TextLow,
        )
        if (blockedAppLabel != null) {
            Text(
                text = "$blockedAppLabel is locked",
                style = MaterialTheme.typography.titleMedium,
                color = RestLockPalette.TextMid,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BlockerHero(preview: ActiveExercisePreview?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(RestLockPalette.Ember, RestLockPalette.EmberDeep),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.FitnessCenter,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(44.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Workout time",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = RestLockPalette.TextHigh,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Finish your set, then pick what comes next.",
                style = MaterialTheme.typography.bodyLarge,
                color = RestLockPalette.TextMid,
                textAlign = TextAlign.Center,
            )
            if (preview != null) {
                Text(
                    text = "${preview.definition.name}\nSet ${preview.setNumberForExercise}/${preview.totalSetsForExercise} · ${preview.plannedExercise.reps} reps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = RestLockPalette.Mint,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BlockerActions(
    setsCompleted: Int,
    extraRests: Int,
    isFinalSet: Boolean,
    onExerciseDone: () -> Unit,
    onAddThirtySeconds: () -> Unit,
    onFinishWorkout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniStat(label = "Sets", value = setsCompleted.toString(), tint = RestLockPalette.Mint)
                Spacer(Modifier.size(24.dp))
                MiniStat(label = "+30s", value = extraRests.toString(), tint = RestLockPalette.Amber)
            }
        }

        PrimaryAction(
            label = if (isFinalSet) "Finish final set" else "Exercise done",
            onClick = onExerciseDone,
            leadingIcon = Icons.Rounded.CheckCircle,
            brush = MintBrush,
            contentColor = RestLockPalette.Ink0,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryAction(
                label = "+30s rest",
                onClick = onAddThirtySeconds,
                leadingIcon = Icons.Rounded.AddAlarm,
                modifier = Modifier.weight(1f),
            )
            SecondaryAction(
                label = "Finish",
                onClick = onFinishWorkout,
                leadingIcon = Icons.Rounded.Stop,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = RestLockPalette.TextLow,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = tint,
        )
    }
}
