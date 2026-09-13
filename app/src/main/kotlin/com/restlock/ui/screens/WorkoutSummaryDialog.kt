package com.restlock.ui.screens

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
                        true -> "Workout complete"
                        false -> "Workout ended early"
                        null -> "Workout finished"
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
                    Text("Duration: ${log.durationLabel()}", color = RestLockPalette.TextMid)
                    Text(log.setsLabel(), color = RestLockPalette.TextMid)
                    Text("${log.exerciseCount} exercises reached", color = RestLockPalette.TextMid)
                    Text(
                        text = "~${log.calories} kcal",
                        style = MaterialTheme.typography.titleLarge,
                        color = RestLockPalette.Mint,
                    )
                    Text(
                        text = "Saved to your recent workouts. Calories are approximate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RestLockPalette.TextLow,
                    )
                }
                PrimaryAction(
                    label = "Done",
                    onClick = onDismiss,
                    brush = MintBrush,
                    contentColor = RestLockPalette.Ink0,
                )
                SecondaryAction(label = "Support the creator (optional)", onClick = onSupportCreator)
            }
        }
    }
}

internal fun WorkoutLog.durationLabel(): String = durationMinutes?.let { "$it min" } ?: "Duration unavailable"

internal fun WorkoutLog.setsLabel(): String = if (completedSets != null && plannedSets != null) {
    "$completedSets/$plannedSets sets completed"
} else {
    "Set counts unavailable"
}

internal fun WorkoutLog.completionLabel(): String = when (completedFully) {
    true -> "Completed"
    false -> "Ended early"
    null -> "Completion unknown"
}
