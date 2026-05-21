package com.restlock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.restlock.domain.SettingsRepository
import com.restlock.ui.components.PrimaryAction
import com.restlock.ui.components.SecondaryAction
import com.restlock.ui.components.toCompactLabel
import com.restlock.ui.theme.PrimaryBrush
import com.restlock.ui.theme.RestLockPalette
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val MIN_CUSTOM_REST_SECONDS = 5
private const val MAX_CUSTOM_REST_SECONDS = 3_600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupSheet(
    initialRest: Duration,
    allowedCount: Int,
    onDismiss: () -> Unit,
    onOpenAppPicker: () -> Unit,
    onStart: (Duration) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember(initialRest) { mutableStateOf(initialRest) }
    val initialIsPreset = remember(initialRest) { SettingsRepository.RestPresets.contains(initialRest) }
    var customOpen by remember(initialRest) { mutableStateOf(!initialIsPreset) }
    var customMinutes by remember(initialRest) {
        mutableStateOf(if (initialIsPreset) "" else (initialRest.inWholeSeconds / 60).toString())
    }
    var customSeconds by remember(initialRest) {
        mutableStateOf(if (initialIsPreset) "" else (initialRest.inWholeSeconds % 60).toString())
    }
    var customError by remember(initialRest) { mutableStateOf<String?>(null) }
    val selectedIsPreset = SettingsRepository.RestPresets.contains(selected)

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
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.18f))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Set up your session",
                    style = MaterialTheme.typography.headlineMedium,
                    color = RestLockPalette.TextHigh,
                )
                Text(
                    text = "Choose your rest length. Apps lock when the timer ends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextMid,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "REST DURATION",
                    style = MaterialTheme.typography.labelMedium,
                    color = RestLockPalette.TextLow,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(SettingsRepository.RestPresets) { duration ->
                        DurationChip(
                            label = duration.toCompactLabel(),
                            selected = duration == selected,
                            onClick = { selected = duration },
                        )
                    }
                    item {
                        DurationChip(
                            label = "Custom",
                            selected = !selectedIsPreset,
                            onClick = {
                                customOpen = !customOpen
                                if (customMinutes.isBlank() && customSeconds.isBlank()) {
                                    val totalSeconds = selected.inWholeSeconds.coerceIn(
                                        MIN_CUSTOM_REST_SECONDS.toLong(),
                                        MAX_CUSTOM_REST_SECONDS.toLong(),
                                    )
                                    customMinutes = (totalSeconds / 60).toString()
                                    customSeconds = (totalSeconds % 60).toString()
                                }
                            },
                        )
                    }
                }

                if (customOpen) {
                    CustomDurationEditor(
                        minutesText = customMinutes,
                        secondsText = customSeconds,
                        errorText = customError,
                        onMinutesChange = {
                            customMinutes = it.onlyDigits(maxLength = 2)
                            customError = null
                        },
                        onSecondsChange = {
                            customSeconds = it.onlyDigits(maxLength = 2)
                            customError = null
                        },
                        onApply = {
                            val customSecondsTotal = parseCustomDurationSeconds(
                                minutesText = customMinutes,
                                secondsText = customSeconds,
                            )
                            if (customSecondsTotal == null) {
                                customError = "Use a time from 5 sec to 60 min."
                            } else {
                                selected = customSecondsTotal.seconds
                                customError = null
                            }
                        },
                    )
                }
            }

            AllowedAppsRow(
                allowedCount = allowedCount,
                onOpenAppPicker = onOpenAppPicker,
            )

            Spacer(Modifier.height(4.dp))

            PrimaryAction(
                label = "Start workout",
                onClick = { onStart(selected) },
                leadingIcon = Icons.Rounded.PlayArrow,
                brush = PrimaryBrush,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CustomDurationEditor(
    minutesText: String,
    secondsText: String,
    errorText: String?,
    onMinutesChange: (String) -> Unit,
    onSecondsChange: (String) -> Unit,
    onApply: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DurationNumberField(
                value = minutesText,
                label = "Min",
                isError = errorText != null,
                onValueChange = onMinutesChange,
                modifier = Modifier.weight(1f),
            )
            DurationNumberField(
                value = secondsText,
                label = "Sec",
                isError = errorText != null,
                onValueChange = onSecondsChange,
                modifier = Modifier.weight(1f),
            )
        }
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodyMedium,
                color = RestLockPalette.Coral,
            )
        }
        SecondaryAction(
            label = "Use custom time",
            onClick = onApply,
        )
    }
}

@Composable
private fun DurationNumberField(
    value: String,
    label: String,
    isError: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = RestLockPalette.TextHigh,
            unfocusedTextColor = RestLockPalette.TextHigh,
            focusedLabelColor = RestLockPalette.TextMid,
            unfocusedLabelColor = RestLockPalette.TextLow,
            cursorColor = RestLockPalette.Violet,
            focusedBorderColor = RestLockPalette.Violet,
            unfocusedBorderColor = RestLockPalette.GlassOutline,
            errorBorderColor = RestLockPalette.Coral,
            focusedContainerColor = Color.White.copy(alpha = 0.04f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
            errorContainerColor = Color.White.copy(alpha = 0.03f),
        ),
    )
}

@Composable
private fun DurationChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f)
    val borderColor = if (selected) RestLockPalette.Violet else Color.Transparent
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(width = if (selected) 1.5.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) RestLockPalette.TextHigh else RestLockPalette.TextMid,
        )
    }
}

private fun String.onlyDigits(maxLength: Int): String {
    return filter { it.isDigit() }.take(maxLength)
}

private fun parseCustomDurationSeconds(
    minutesText: String,
    secondsText: String,
): Int? {
    val minutes = minutesText.toIntOrNull() ?: 0
    val seconds = secondsText.toIntOrNull() ?: 0
    val totalSeconds = minutes * 60 + seconds
    return totalSeconds.takeIf { it in MIN_CUSTOM_REST_SECONDS..MAX_CUSTOM_REST_SECONDS }
}

@Composable
private fun AllowedAppsRow(
    allowedCount: Int,
    onOpenAppPicker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ALLOWED APPS",
                style = MaterialTheme.typography.labelMedium,
                color = RestLockPalette.TextLow,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (allowedCount == 0) "Strict mode" else "$allowedCount allowed",
                style = MaterialTheme.typography.titleMedium,
                color = RestLockPalette.TextHigh,
            )
        }
        SecondaryAction(
            label = if (allowedCount == 0) "Allow apps" else "Edit",
            onClick = onOpenAppPicker,
            leadingIcon = Icons.Rounded.Apps,
            modifier = Modifier.width(160.dp),
        )
    }
}
