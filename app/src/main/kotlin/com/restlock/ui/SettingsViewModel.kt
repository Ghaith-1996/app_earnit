package com.restlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restlock.domain.FitnessCalculator
import com.restlock.domain.FitnessRepository
import com.restlock.domain.UserProfile
import com.restlock.domain.UserSex
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: UserProfile = UserProfile(),
    val restingMetabolicRate: Int? = null,
)

class SettingsViewModel(
    private val fitnessRepository: FitnessRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = fitnessRepository.userProfile
        .map { profile ->
            SettingsUiState(
                profile = profile,
                restingMetabolicRate = FitnessCalculator.restingMetabolicRate(profile),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun saveProfile(
        ageYears: Int?,
        sex: UserSex,
        weightKg: Double?,
        heightCm: Int?,
    ) {
        viewModelScope.launch {
            fitnessRepository.saveUserProfile(
                UserProfile(
                    ageYears = ageYears?.takeIf { it in 13..120 },
                    sex = sex,
                    weightKg = weightKg?.takeIf { it in 30.0..300.0 },
                    heightCm = heightCm?.takeIf { it in 100..240 },
                )
            )
        }
    }
}
