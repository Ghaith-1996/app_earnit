package com.restlock.domain.backend

import com.fitness.restlock.backend.WorkoutController
import com.restlock.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class BackendSettingsRepository(
    private val controller: WorkoutController,
) : SettingsRepository {
    override val chosenRest: Flow<Duration> = controller.state
        .map { it.restDurationSeconds.seconds }
        .distinctUntilChanged()

    override val allowedPackages: Flow<Set<String>> = controller.state
        .map { it.allowedApps }
        .distinctUntilChanged()

    override suspend fun setChosenRest(duration: Duration) {
        controller.setRestDuration(duration.inWholeSeconds.toInt())
    }

    override suspend fun setAllowedPackages(packages: Set<String>) {
        controller.setAllowedApps(packages)
    }
}
