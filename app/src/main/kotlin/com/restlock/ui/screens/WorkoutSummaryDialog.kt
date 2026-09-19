package com.restlock.ui.screens

import com.fitness.restlock.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.restlock.domain.WorkoutLog
import com.restlock.ui.components.GlassCard
import com.restlock.ui.components.PrimaryAction
import com.restlock.ui.components.SecondaryAction
import com.restlock.ui.theme.MintBrush
import com.restlock.ui.theme.RestLockPalette

/** Presents the same persisted result that appears in recent history. */
@Composable
fun WorkoutSummaryDialog(
    log: WorkoutLog,
    onDismiss: () -> Unit,
    onSupportCreator: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            cornerRadius = 24,
            contentPadding = 20,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = when (log.completedFully) {
                        true -> stringResource(R.string.summary_complete)
                        false -> stringResource(R.string.summary_ended_early)
                        null -> stringResource(R.string.summary_finished)
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = RestLockPalette.TextHigh,
                )
                Text(
                    text = log.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = RestLockPalette.TextHigh,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.summary_duration, log.durationLabel()), color = RestLockPalette.TextMid)
                    Text(log.setsLabel(), color = RestLockPalette.TextMid)
                    Text(pluralStringResource(R.plurals.summary_exercises_reached, log.exerciseCount, log.exerciseCount), color = RestLockPalette.TextMid)
                    Text(
                        text = stringResource(R.string.calories_estimate, log.calories),
                        style = MaterialTheme.typography.titleLarge,
                        color = RestLockPalette.Mint,
                    )
                    Text(
                        text = stringResource(R.string.summary_saved_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = RestLockPalette.TextLow,
                    )
                }
                PrimaryAction(
                    label = stringResource(R.string.action_done),
                    onClick = onDismiss,
                    brush = MintBrush,
                    contentColor = RestLockPalette.Ink0,
                )
                SecondaryAction(label = stringResource(R.string.action_support_optional), onClick = onSupportCreator)
            }
        }
    }
}

@Composable
internal fun WorkoutLog.durationLabel(): String = durationMinutes?.let { stringResource(R.string.duration_minutes, it) } ?: stringResource(R.string.duration_unavailable)

@Composable
internal fun WorkoutLog.setsLabel(): String = if (completedSets != null && plannedSets != null) {
    pluralStringResource(R.plurals.sets_completed, plannedSets, completedSets, plannedSets)
} else {
    stringResource(R.string.sets_unavailable)
}

@Composable
internal fun WorkoutLog.completionLabel(): String = when (completedFully) {
    true -> stringResource(R.string.completion_completed)
    false -> stringResource(R.string.completion_ended_early)
    null -> stringResource(R.string.completion_unknown)
}
