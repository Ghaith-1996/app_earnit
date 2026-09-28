package com.restlock.ui.screens

import com.fitness.restlock.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.restlock.domain.UserProfile
import com.restlock.domain.UserSex
import com.restlock.ui.SettingsViewModel
import com.restlock.ui.components.GlassCard
import com.restlock.ui.components.PrimaryAction
import com.restlock.ui.theme.PrimaryBrush
import com.restlock.ui.theme.RestLockPalette

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onHome: () -> Unit,
    onWorkouts: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile
    var ageText by remember(profile.ageYears) { mutableStateOf(profile.ageYears?.toString().orEmpty()) }
    var sex by remember(profile.sex) { mutableStateOf(profile.sex) }
    var weightText by remember(profile.weightKg) { mutableStateOf(profile.weightKg?.toString().orEmpty()) }
    var heightText by remember(profile.heightCm) { mutableStateOf(profile.heightCm?.toString().orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        SettingsTopBar()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileCard(
                ageText = ageText,
                sex = sex,
                weightText = weightText,
                heightText = heightText,
                onAgeChange = { ageText = it.filter(Char::isDigit).take(3) },
                onSexChange = { sex = it },
                onWeightChange = { weightText = it.filterDecimal().take(6) },
                onHeightChange = { heightText = it.filter(Char::isDigit).take(3) },
                onSave = {
                    viewModel.saveProfile(
                        ageYears = ageText.toIntOrNull(),
                        sex = sex,
                        weightKg = weightText.toDoubleOrNull(),
                        heightCm = heightText.toIntOrNull(),
                    )
                },
            )

            EstimateCard(
                profile = profile,
                restingMetabolicRate = state.restingMetabolicRate,
            )
        }

        Spacer(Modifier.height(12.dp))
        MainBottomBar(
            selectedTab = MainTab.Settings,
            onHome = onHome,
            onWorkouts = onWorkouts,
            onSettings = {},
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = RestLockPalette.TextHigh,
        )
        Text(
            text = stringResource(R.string.settings_description),
            style = MaterialTheme.typography.bodyMedium,
            color = RestLockPalette.TextLow,
        )
    }
}

@Composable
private fun ProfileCard(
    ageText: String,
    sex: UserSex,
    weightText: String,
    heightText: String,
    onAgeChange: (String) -> Unit,
    onSexChange: (UserSex) -> Unit,
    onWeightChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.settings_profile_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = RestLockPalette.TextHigh,
                )
                Text(
                    text = stringResource(R.string.settings_profile_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextLow,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileNumberField(
                    value = ageText,
                    onValueChange = onAgeChange,
                    label = stringResource(R.string.settings_age),
                    modifier = Modifier.weight(1f),
                )
                ProfileNumberField(
                    value = heightText,
                    onValueChange = onHeightChange,
                    label = stringResource(R.string.settings_height),
                    modifier = Modifier.weight(1f),
                )
            }

            ProfileNumberField(
                value = weightText,
                onValueChange = onWeightChange,
                label = stringResource(R.string.settings_weight),
                modifier = Modifier.fillMaxWidth(),
                decimal = true,
            )

            SexPicker(selectedSex = sex, onSexChange = onSexChange)

            PrimaryAction(
                label = stringResource(R.string.action_save_settings),
                onClick = onSave,
                leadingIcon = Icons.Rounded.Save,
                brush = PrimaryBrush,
            )
        }
    }
}

@Composable
private fun ProfileNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
    )
}

@Composable
private fun SexPicker(
    selectedSex: UserSex,
    onSexChange: (UserSex) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(UserSex.Unspecified, UserSex.Male, UserSex.Female).forEach { sex ->
            val selected = sex == selectedSex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) RestLockPalette.Mint else Color.White.copy(alpha = 0.07f)
                    )
                    .clickable { onSexChange(sex) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(when (sex) {
                        UserSex.Unspecified -> R.string.settings_sex_unspecified
                        UserSex.Male -> R.string.settings_sex_male
                        UserSex.Female -> R.string.settings_sex_female
                    }),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) RestLockPalette.Ink0 else RestLockPalette.TextMid,
                )
            }
        }
    }
}

@Composable
private fun EstimateCard(
    profile: UserProfile,
    restingMetabolicRate: Int?,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.settings_calorie_model),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
            )
            Text(
                text = if (restingMetabolicRate == null) {
                    stringResource(R.string.settings_incomplete_profile)
                } else {
                    stringResource(R.string.settings_metabolic_rate, restingMetabolicRate)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = RestLockPalette.TextLow,
            )
            Text(
                text = stringResource(
                    R.string.settings_current_profile,
                    profile.weightKg?.let { stringResource(R.string.weight_kg, it.toString()) } ?: stringResource(R.string.settings_weight_not_set),
                    profile.heightCm?.let { stringResource(R.string.height_cm, it) } ?: stringResource(R.string.settings_height_not_set),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = RestLockPalette.TextMid,
            )
        }
    }
}

private fun String.filterDecimal(): String {
    var dotSeen = false
    return filter { char ->
        when {
            char.isDigit() -> true
            char == '.' && !dotSeen -> {
                dotSeen = true
                true
            }
            else -> false
        }
    }
}
