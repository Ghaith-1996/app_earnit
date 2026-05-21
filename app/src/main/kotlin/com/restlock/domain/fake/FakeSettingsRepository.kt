package com.restlock.domain.fake

import com.restlock.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration

class FakeSettingsRepository : SettingsRepository {
    private val _rest = MutableStateFlow(SettingsRepository.DefaultRest)
    private val _allowed = MutableStateFlow<Set<String>>(emptySet())

    override val chosenRest: Flow<Duration> = _rest.asStateFlow()
    override val allowedPackages: Flow<Set<String>> = _allowed.asStateFlow()

    override suspend fun setChosenRest(duration: Duration) {
        _rest.value = duration
    }

    override suspend fun setAllowedPackages(packages: Set<String>) {
        _allowed.value = packages
    }
}
