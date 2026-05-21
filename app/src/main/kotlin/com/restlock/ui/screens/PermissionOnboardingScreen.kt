package com.restlock.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
import com.restlock.ui.HomeViewModel
import com.restlock.ui.components.GlassCard
import com.restlock.ui.components.PrimaryAction
import com.restlock.ui.components.SecondaryAction
import com.restlock.ui.theme.PrimaryBrush
import com.restlock.ui.theme.RestLockPalette

@Composable
fun PermissionOnboardingScreen(
    viewModel: HomeViewModel,
    onContinue: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var consentChecked by remember { mutableStateOf(false) }
    val accessibilityEnabled = state.permissions.isAccessibilityServiceEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Enable app locking",
                style = MaterialTheme.typography.headlineMedium,
                color = RestLockPalette.TextHigh,
            )
            Text(
                text = "Rest Lock needs Accessibility only to detect blocked apps after your rest timer reaches 0:00.",
                style = MaterialTheme.typography.bodyMedium,
                color = RestLockPalette.TextMid,
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PermissionStatusRow(enabled = accessibilityEnabled)
                DisclosurePoint(
                    title = "When it runs",
                    body = "Only during the decision lock after a rest timer expires.",
                )
                DisclosurePoint(
                    title = "What it checks",
                    body = "The package name of the app currently in front, so Rest Lock can compare it with your allowed apps.",
                )
                DisclosurePoint(
                    title = "What it does",
                    body = "If the app is not allowed, Rest Lock sends you back to Home and opens the workout decision screen.",
                )
                DisclosurePoint(
                    title = "What it does not do",
                    body = "It does not read messages, passwords, form text, notifications, or screen content for ads or analytics.",
                )
                DisclosurePoint(
                    title = "Where data stays",
                    body = "Your rest time, allowed apps, and current session are stored locally on this device.",
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 16) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Checkbox(
                    checked = consentChecked,
                    onCheckedChange = { consentChecked = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = RestLockPalette.Violet,
                        uncheckedColor = RestLockPalette.TextLow,
                        checkmarkColor = RestLockPalette.TextHigh,
                    ),
                )
                Text(
                    text = "I understand and want to enable Accessibility for app locking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextHigh,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        PrimaryAction(
            label = if (accessibilityEnabled) "Accessibility enabled" else "Open Accessibility settings",
            onClick = {
                viewModel.refreshPermissions()
                context.startActivity(viewModel.accessibilitySettingsIntent().newTask())
            },
            leadingIcon = if (accessibilityEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Tune,
            enabled = consentChecked || accessibilityEnabled,
            brush = PrimaryBrush,
        )

        SecondaryAction(
            label = "Continue to app",
            onClick = {
                viewModel.refreshPermissions()
                onContinue()
            },
        )

        Text(
            text = "Exact alarm permission is not requested. If Android does not allow precise alarms, Rest Lock uses the system fallback timer.",
            style = MaterialTheme.typography.bodyMedium,
            color = RestLockPalette.TextLow,
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionStatusRow(enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.Tune,
            contentDescription = null,
            tint = if (enabled) RestLockPalette.Mint else RestLockPalette.Amber,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (enabled) "Accessibility is enabled" else "Accessibility is off",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
            )
            Text(
                text = if (enabled) "Blocking can run when the rest timer ends."
                else "Blocking will not work until this service is enabled in Android settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = RestLockPalette.TextLow,
            )
        }
    }
}

@Composable
private fun DisclosurePoint(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = RestLockPalette.TextLow,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = RestLockPalette.TextHigh,
        )
    }
}

private fun Intent.newTask(): Intent = addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
