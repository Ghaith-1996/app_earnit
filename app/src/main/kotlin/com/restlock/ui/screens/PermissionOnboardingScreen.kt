package com.restlock.ui.screens

import com.fitness.restlock.R
import androidx.compose.ui.res.stringResource

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
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineMedium,
                color = RestLockPalette.TextHigh,
            )
            Text(
                text = stringResource(R.string.permission_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = RestLockPalette.TextMid,
            )
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PermissionStatusRow(enabled = accessibilityEnabled)
                DisclosurePoint(
                    title = stringResource(R.string.permission_timing_title),
                    body = stringResource(R.string.permission_timing_body),
                )
                DisclosurePoint(
                    title = stringResource(R.string.permission_observation_title),
                    body = stringResource(R.string.permission_observation_body),
                )
                DisclosurePoint(
                    title = stringResource(R.string.permission_action_title),
                    body = stringResource(R.string.permission_action_body),
                )
                DisclosurePoint(
                    title = stringResource(R.string.permission_exclusions_title),
                    body = stringResource(R.string.permission_exclusions_body),
                )
                DisclosurePoint(
                    title = stringResource(R.string.permission_storage_title),
                    body = stringResource(R.string.permission_storage_body),
                )
                DisclosurePoint(
                    title = stringResource(R.string.permission_ads_title),
                    body = stringResource(R.string.permission_ads_body),
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
                    text = stringResource(R.string.permission_consent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextHigh,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        PrimaryAction(
            label = if (accessibilityEnabled) stringResource(R.string.permission_enabled_action) else stringResource(R.string.action_open_accessibility_settings),
            onClick = {
                viewModel.refreshPermissions()
                context.startActivity(viewModel.accessibilitySettingsIntent().newTask())
            },
            leadingIcon = if (accessibilityEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.Tune,
            enabled = consentChecked || accessibilityEnabled,
            brush = PrimaryBrush,
        )

        SecondaryAction(
            label = stringResource(R.string.action_continue_app),
            onClick = {
                viewModel.refreshPermissions()
                onContinue()
            },
        )

        Text(
            text = stringResource(R.string.permission_alarm_note),
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
                text = if (enabled) stringResource(R.string.permission_enabled) else stringResource(R.string.permission_disabled),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
            )
            Text(
                text = if (enabled) stringResource(R.string.permission_ready)
                else stringResource(R.string.permission_needs_enable),
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
