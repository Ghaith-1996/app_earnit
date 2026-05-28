package com.restlock.ui

import android.content.Intent
import com.fitness.restlock.backend.PermissionStatus
import com.fitness.restlock.backend.permissions.PermissionGateway
import com.google.common.truth.Truth.assertThat
import com.restlock.domain.FitnessCalculator
import com.restlock.domain.FitnessRepository
import com.restlock.domain.PlannedExercise
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.SessionEngine
import com.restlock.domain.SessionState
import com.restlock.domain.SettingsRepository
import com.restlock.domain.UserProfile
import com.restlock.domain.WorkoutLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutFinishLoggingTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `startSavedWorkout persists active workout id and starts session`() = runTest {
        val fitnessRepository = FakeFitnessRepository()
        val workout = sampleWorkout()
        fitnessRepository.saveWorkout(workout)
        val sessionEngine = TestSessionEngine()
        val viewModel = homeViewModel(
            fitnessRepository = fitnessRepository,
            sessionEngine = sessionEngine,
        )
        val collection = backgroundScope.launch(mainDispatcherRule.dispatcher) {
            viewModel.uiState.collect()
        }
        runCurrent()

        viewModel.startSavedWorkout(workout.id, 60.seconds)
        runCurrent()

        assertThat(fitnessRepository.activeWorkoutId.value).isEqualTo(workout.id)
        assertThat(sessionEngine.startedRest).isEqualTo(60.seconds)
        collection.cancel()
    }

    @Test
    fun `finish from Home logs active workout and clears active id`() = runTest {
        val fitnessRepository = FakeFitnessRepository()
        val workout = sampleWorkout()
        fitnessRepository.saveWorkout(workout)
        fitnessRepository.setActiveWorkoutId(workout.id)
        val sessionEngine = TestSessionEngine()
        val viewModel = homeViewModel(
            fitnessRepository = fitnessRepository,
            sessionEngine = sessionEngine,
        )

        viewModel.finishWorkout()
        runCurrent()

        val logs = fitnessRepository.workoutLogs.value
        assertThat(logs).hasSize(1)
        assertThat(logs.single().name).isEqualTo(workout.name)
        assertThat(logs.single().exerciseCount).isEqualTo(workout.exerciseIds.size)
        assertThat(fitnessRepository.activeWorkoutId.value).isNull()
        assertThat(sessionEngine.finishCalls).isEqualTo(1)
    }

    @Test
    fun `finish from Blocker logs active workout and clears active id before callback`() = runTest {
        val fitnessRepository = FakeFitnessRepository()
        val workout = sampleWorkout()
        fitnessRepository.saveWorkout(workout)
        fitnessRepository.setActiveWorkoutId(workout.id)
        val sessionEngine = TestSessionEngine()
        val viewModel = BlockerViewModel(
            sessionEngine = sessionEngine,
            fitnessRepository = fitnessRepository,
        )
        var callbackSawFinishedState = false

        viewModel.finishWorkout {
            callbackSawFinishedState = sessionEngine.finishCalls == 1 &&
                fitnessRepository.activeWorkoutId.value == null &&
                fitnessRepository.workoutLogs.value.size == 1
        }
        runCurrent()

        assertThat(callbackSawFinishedState).isTrue()
        assertThat(fitnessRepository.workoutLogs.value.single().name).isEqualTo(workout.name)
    }

    @Test
    fun `finish without active workout does not log and still finishes session`() = runTest {
        val fitnessRepository = FakeFitnessRepository()
        val sessionEngine = TestSessionEngine()
        val viewModel = BlockerViewModel(
            sessionEngine = sessionEngine,
            fitnessRepository = fitnessRepository,
        )

        viewModel.finishWorkout()
        runCurrent()

        assertThat(fitnessRepository.workoutLogs.value).isEmpty()
        assertThat(fitnessRepository.activeWorkoutId.value).isNull()
        assertThat(sessionEngine.finishCalls).isEqualTo(1)
    }

    @Test
    fun `missing active workout is cleared without crash or log`() = runTest {
        val fitnessRepository = FakeFitnessRepository()
        fitnessRepository.setActiveWorkoutId("missing-workout")
        val sessionEngine = TestSessionEngine()
        val viewModel = BlockerViewModel(
            sessionEngine = sessionEngine,
            fitnessRepository = fitnessRepository,
        )

        viewModel.finishWorkout()
        runCurrent()

        assertThat(fitnessRepository.workoutLogs.value).isEmpty()
        assertThat(fitnessRepository.activeWorkoutId.value).isNull()
        assertThat(sessionEngine.finishCalls).isEqualTo(1)
    }

    private fun homeViewModel(
        fitnessRepository: FakeFitnessRepository,
        sessionEngine: TestSessionEngine = TestSessionEngine(),
    ): HomeViewModel {
        return HomeViewModel(
            sessionEngine = sessionEngine,
            settingsRepository = FakeSettingsRepository(),
            fitnessRepository = fitnessRepository,
            permissionGateway = FakePermissionGateway(),
            refreshPermissionsPeriodically = false,
        )
    }

    private fun sampleWorkout(): PlannedWorkout {
        return PlannedWorkout(
            id = "push-day",
            name = "Push day",
            exercises = listOf(
                PlannedExercise(exerciseId = "bench_press", sets = 3, reps = 8, rank = 1),
                PlannedExercise(exerciseId = "overhead_press", sets = 3, reps = 10, rank = 2),
            ),
            createdAtMillis = 1_000L,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    val dispatcher = UnconfinedTestDispatcher()

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeFitnessRepository : FitnessRepository {
    private val userProfileState = MutableStateFlow(UserProfile(weightKg = 70.0))
    private val savedWorkoutsState = MutableStateFlow<List<PlannedWorkout>>(emptyList())
    private val workoutLogsState = MutableStateFlow<List<WorkoutLog>>(emptyList())
    private val activeWorkoutIdState = MutableStateFlow<String?>(null)

    override val userProfile: StateFlow<UserProfile> = userProfileState.asStateFlow()
    override val savedWorkouts: StateFlow<List<PlannedWorkout>> = savedWorkoutsState.asStateFlow()
    override val workoutLogs: StateFlow<List<WorkoutLog>> = workoutLogsState.asStateFlow()
    override val activeWorkoutId: StateFlow<String?> = activeWorkoutIdState.asStateFlow()

    override suspend fun saveUserProfile(profile: UserProfile) {
        userProfileState.value = profile
    }

    override suspend fun saveWorkout(workout: PlannedWorkout) {
        savedWorkoutsState.value = listOf(workout) + savedWorkoutsState.value.filterNot { it.id == workout.id }
    }

    override suspend fun logWorkout(log: WorkoutLog) {
        workoutLogsState.value = listOf(log) + workoutLogsState.value
    }

    override suspend fun setActiveWorkoutId(workoutId: String?) {
        activeWorkoutIdState.value = workoutId
    }

    override suspend fun logActiveWorkoutAndClear(completedAtMillis: Long): Boolean {
        val workoutId = activeWorkoutIdState.value
        if (workoutId.isNullOrBlank()) {
            activeWorkoutIdState.value = null
            return false
        }

        val workout = savedWorkoutsState.value.firstOrNull { it.id == workoutId }
        if (workout == null) {
            activeWorkoutIdState.value = null
            return false
        }

        logWorkout(
            WorkoutLog(
                name = workout.name,
                completedAtMillis = completedAtMillis,
                durationMinutes = FitnessCalculator.durationForPlannedExercises(workout.exercises),
                calories = FitnessCalculator.caloriesForPlannedExercises(workout.exercises, userProfileState.value),
                exerciseCount = workout.exerciseIds.size,
            )
        )
        activeWorkoutIdState.value = null
        return true
    }
}

private class TestSessionEngine : SessionEngine {
    private val stateFlow = MutableStateFlow(
        SessionState(
            phase = SessionState.Phase.Idle,
            chosenRest = SettingsRepository.DefaultRest,
        )
    )

    override val state: StateFlow<SessionState> = stateFlow.asStateFlow()

    var startedRest: Duration? = null
        private set

    var finishCalls: Int = 0
        private set

    override fun startWorkout(rest: Duration) {
        startedRest = rest
        stateFlow.value = stateFlow.value.copy(
            phase = SessionState.Phase.Resting,
            chosenRest = rest,
            remaining = rest,
        )
    }

    override fun addThirtySeconds() = Unit

    override fun exerciseDone() = Unit

    override fun finishWorkout() {
        finishCalls += 1
        stateFlow.value = stateFlow.value.copy(
            phase = SessionState.Phase.Idle,
            remaining = null,
        )
    }
}

private class FakeSettingsRepository : SettingsRepository {
    private val chosenRestState = MutableStateFlow(SettingsRepository.DefaultRest)
    private val allowedPackagesState = MutableStateFlow<Set<String>>(emptySet())

    override val chosenRest: Flow<Duration> = chosenRestState.asStateFlow()
    override val allowedPackages: Flow<Set<String>> = allowedPackagesState.asStateFlow()

    override suspend fun setChosenRest(duration: Duration) {
        chosenRestState.value = duration
    }

    override suspend fun setAllowedPackages(packages: Set<String>) {
        allowedPackagesState.value = packages
    }
}

private class FakePermissionGateway : PermissionGateway {
    override fun currentStatus(): PermissionStatus = PermissionStatus(
        isAccessibilityServiceEnabled = true,
    )

    override fun accessibilitySettingsIntent(): Intent = Intent()

    override fun appDetailsSettingsIntent(): Intent = Intent()
}
