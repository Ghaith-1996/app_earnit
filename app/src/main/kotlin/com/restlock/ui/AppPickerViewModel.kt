package com.restlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restlock.domain.InstalledApp
import com.restlock.domain.InstalledAppsProvider
import com.restlock.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppPickerUiState(
    val loading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val allowed: Set<String> = emptySet(),
    val query: String = "",
) {
    val filtered: List<InstalledApp>
        get() = if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
}

class AppPickerViewModel(
    private val installedAppsProvider: InstalledAppsProvider,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppPickerUiState())
    val state: StateFlow<AppPickerUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val apps = installedAppsProvider.listLaunchableApps()
                .sortedBy { it.label.lowercase() }
            val allowed = settingsRepository.allowedPackages.first()
            _state.update {
                it.copy(loading = false, apps = apps, allowed = allowed)
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun toggle(pkg: String) {
        _state.update { current ->
            val next = current.allowed.toMutableSet().apply {
                if (!add(pkg)) remove(pkg)
            }
            current.copy(allowed = next)
        }
    }

    fun clearAllowed() {
        _state.update { it.copy(allowed = emptySet()) }
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setAllowedPackages(_state.value.allowed)
            onSaved()
        }
    }
}
