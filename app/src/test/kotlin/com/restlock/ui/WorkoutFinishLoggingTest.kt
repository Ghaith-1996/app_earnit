package com.restlock.ui

import android.content.Intent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.fitness.restlock.backend.PermissionStatus
import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.alarm.RestAlarmScheduler
import com.fitness.restlock.backend.blocking.StaticAppBlockPolicy
import com.fitness.restlock.backend.permissions.PermissionGateway
import com.fitness.restlock.backend.persistence.WorkoutPreferencesStore
import com.fitness.restlock.backend.session.Clock
import com.fitness.restlock.backend.session.DefaultWorkoutController
import com.google.common.truth.Truth.assertThat
import com.restlock.domain.*
import com.restlock.domain.backend.BackendSessionEngine
import com.restlock.domain.fake.FakeSettingsRepository
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutFinishLoggingTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `saved launch works before Home UI subscribes and does not log a completion`() = runTest {
        val fixture = fixture()
        fixture.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        assertThat(fixture.repository.activeWorkoutId.value).isEqualTo("legs")
        assertThat(fixture.engine.state.value.phase).isEqualTo(SessionState.Phase.Resting)
        assertThat(fixture.engine.state.value.setsCompleted).isEqualTo(0)
        assertThat(fixture.engine.state.value.plannedSets).isEqualTo(3)
        assertThat(fixture.engine.state.value.remaining).isEqualTo(5.seconds)
        assertThat(fixture.engine.state.value.blockerArmed).isFalse()
        assertThat(fixture.repository.workoutLogs.value).isEmpty()
        assertThat(fixture.alarm.deadline).isEqualTo(5_000L)
    }

    @Test
    fun `Home and Blocker advance every planned set then final set ends without another alarm`() = runTest {
        val f = fixture()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.home.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.blocker.activeExercisePreview.collect() }
        f.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        assertPreview(f, "barbell_squat", 1, 0)
        repeat(2) { index ->
            advanceTimeBy(5_000)
            runCurrent()
            assertThat(f.engine.state.value.phase).isEqualTo(SessionState.Phase.AwaitingDecision)
            assertThat(f.engine.state.value.blockerArmed).isTrue()
            if (index == 0) f.home.exerciseDone() else f.blocker.exerciseDone()
            runCurrent()
            assertThat(f.engine.state.value.phase).isEqualTo(SessionState.Phase.Resting)
            assertThat(f.engine.state.value.blockerArmed).isFalse()
            if (index == 0) assertPreview(f, "barbell_squat", 2, 1)
            else assertPreview(f, "leg_press", 1, 2)
        }
        advanceTimeBy(5_000)
        runCurrent()
        assertThat(f.engine.state.value.isFinalSet).isTrue()
        f.blocker.exerciseDone()
        f.home.exerciseDone() // A simultaneous duplicate must not count or finish twice.
        runCurrent()
        assertFinished(f)
        assertThat(f.repository.workoutLogs.value).hasSize(1)
        assertThat(f.home.uiState.value.activeExercisePreview).isNull()
        advanceTimeBy(60_000)
        runCurrent()
        assertFinished(f)
    }

    @Test
    fun `one set workout can add rest on final set and finish from Home`() = runTest {
        val f = fixture(workout(sets = 1, secondExercise = false))
        f.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        f.blocker.addThirtySeconds()
        runCurrent()
        assertThat(f.engine.state.value.setsCompleted).isEqualTo(0)
        assertThat(f.engine.state.value.extraRests).isEqualTo(1)
        assertThat(f.engine.state.value.remaining).isEqualTo(30.seconds)
        assertThat(f.engine.state.value.blockerArmed).isFalse()
        advanceTimeBy(30_000)
        runCurrent()
        f.home.exerciseDone()
        runCurrent()
        assertFinished(f)
        assertThat(f.repository.workoutLogs.value).hasSize(1)
    }

    @Test
    fun `duplicate launches preserve the running plan timer rest duration and progress`() = runTest {
        val f = fixture()
        f.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        f.home.exerciseDone()
        runCurrent()
        val before = f.engine.state.value
        f.repository.saveWorkout(workout().copy(id = "other"))
        f.home.startSavedWorkout("other", 60.seconds)
        f.home.startWorkout(90.seconds)
        runCurrent()
        assertThat(f.engine.state.value).isEqualTo(before)
        assertThat(f.repository.activeWorkoutId.value).isEqualTo("legs")
        assertThat(f.alarm.deadline).isEqualTo(10_000L)
    }

    @Test
    fun `missing empty and unknown exercise plans do not launch or log`() = runTest {
        val f = fixture()
        for (invalid in listOf(
            workout().copy(id = "empty", exercises = emptyList()),
            workout().copy(id = "unknown", exercises = listOf(PlannedExercise("missing"))),
        )) f.repository.saveWorkout(invalid)
        for (id in listOf("missing", "empty", "unknown")) f.home.startSavedWorkout(id, 5.seconds)
        runCurrent()
        assertFinished(f)
        assertThat(f.repository.workoutLogs.value).isEmpty()
    }

    @Test
    fun `manual finish from both screens clears plan and preserves existing single log behavior`() = runTest {
        val f = fixture()
        for (fromBlocker in listOf(false, true)) {
            f.home.startSavedWorkout("legs", 5.seconds)
            runCurrent()
            var callbackFinished = false
            if (fromBlocker) {
                advanceTimeBy(5_000)
                runCurrent()
                f.blocker.finishWorkout {
                    callbackFinished = f.repository.activeWorkoutId.value == null && f.alarm.deadline == null
                }
            } else f.home.finishWorkout()
            runCurrent()
            assertFinished(f)
            if (fromBlocker) assertThat(callbackFinished).isTrue()
        }
        assertThat(f.repository.workoutLogs.value).hasSize(2)
        f.blocker.finishWorkout()
        runCurrent()
        assertThat(f.repository.workoutLogs.value).hasSize(2)
    }

    @Test
    fun `quick start after saved completion clears planned limit and remains open ended`() = runTest {
        val f = fixture(workout(sets = 1, secondExercise = false))
        f.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        f.home.exerciseDone()
        runCurrent()
        f.home.startWorkout(5.seconds)
        runCurrent()
        repeat(4) {
            advanceTimeBy(5_000)
            runCurrent()
            f.home.exerciseDone()
            runCurrent()
        }
        assertThat(f.engine.state.value.phase).isEqualTo(SessionState.Phase.Resting)
        assertThat(f.engine.state.value.setsCompleted).isEqualTo(4)
        assertThat(f.engine.state.value.plannedSets).isNull()
        assertThat(f.repository.activeWorkoutId.value).isNull()
        f.home.finishWorkout()
        runCurrent()
        assertThat(f.repository.workoutLogs.value).hasSize(1)
    }

    @Test
    fun `reopening DataStore recovers progress and final set limit after timer expiry`() = runTest {
        val f = fixture()
        f.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        repeat(2) {
            advanceTimeBy(5_000)
            runCurrent()
            f.home.exerciseDone()
            runCurrent()
        }
        assertThat(f.store.snapshots.first().plannedSets).isEqualTo(3)
        f.sessionJob.cancelAndJoin()
        f.storageJob.cancelAndJoin()
        advanceTimeBy(10_000)
        val restored = fixture(file = f.file, repository = f.repository)
        runCurrent()
        assertThat(restored.engine.state.value.phase).isEqualTo(SessionState.Phase.AwaitingDecision)
        assertThat(restored.engine.state.value.setsCompleted).isEqualTo(2)
        assertThat(restored.engine.state.value.isFinalSet).isTrue()
        restored.blocker.exerciseDone()
        runCurrent()
        assertFinished(restored)
        assertThat(restored.store.snapshots.first().plannedSets).isNull()
    }

    @Test
    fun `moving exercises changes the saved execution order in both directions`() = runTest {
        val repository = MemoryFitnessRepository()
        val vm = WorkoutViewModel(repository)
        vm.startEditingWorkout(workout())
        vm.moveExerciseDown("barbell_squat")
        vm.saveWorkout()
        runCurrent()
        assertThat(repository.savedWorkouts.value.single().exerciseIds)
            .containsExactly("leg_press", "barbell_squat").inOrder()
        vm.startEditingWorkout(repository.savedWorkouts.value.single())
        vm.moveExerciseUp("barbell_squat")
        vm.saveWorkout()
        runCurrent()
        val saved = repository.savedWorkouts.value.single()
        assertThat(saved.exerciseIds).containsExactly("barbell_squat", "leg_press").inOrder()
        assertThat(saved.orderedExercises.map { it.sets }).containsExactly(2, 1).inOrder()
        assertThat(saved.orderedExercises.map { it.reps }).containsExactly(8, 12).inOrder()
    }

    @Test
    fun `completion bookkeeping cannot clear the next workout launched while logging`() = runTest {
        val f = fixture(workout(sets = 1, secondExercise = false))
        f.repository.logDelayMillis = 1_000
        f.repository.saveWorkout(workout().copy(id = "next"))
        f.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        f.home.exerciseDone()
        runCurrent()
        assertThat(f.engine.state.value.phase).isEqualTo(SessionState.Phase.Idle)
        f.home.startSavedWorkout("next", 5.seconds)
        advanceTimeBy(1_000)
        runCurrent()
        assertThat(f.repository.activeWorkoutId.value).isEqualTo("next")
        assertThat(f.engine.state.value.phase).isEqualTo(SessionState.Phase.Resting)
        assertThat(f.engine.state.value.plannedSets).isEqualTo(3)
        assertThat(f.repository.workoutLogs.value).hasSize(1)
    }

    @Test
    fun `logging failure still ends final set clears selection and allows another session`() = runTest {
        val f = fixture(workout(sets = 1, secondExercise = false))
        f.repository.failLogging = true
        f.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        f.blocker.exerciseDone()
        runCurrent()
        assertFinished(f)
        f.home.startWorkout(5.seconds)
        runCurrent()
        assertThat(f.engine.state.value.phase).isEqualTo(SessionState.Phase.Resting)
    }

    @Test
    fun `finish clears a missing active workout without logging`() = runTest {
        val f = fixture()
        f.repository.setActiveWorkoutId("deleted")
        f.blocker.finishWorkout()
        runCurrent()
        assertFinished(f)
        assertThat(f.repository.workoutLogs.value).isEmpty()
    }

    private fun assertPreview(f: Fixture, exercise: String, set: Int, completed: Int) {
        val preview = f.home.uiState.value.activeExercisePreview!!
        assertThat(preview.definition.id).isEqualTo(exercise)
        assertThat(preview.setNumberForExercise).isEqualTo(set)
        assertThat(preview.completedSetsInWorkout).isEqualTo(completed)
        assertThat(preview.totalSetsInWorkout).isEqualTo(3)
        assertThat(f.blocker.activeExercisePreview.value).isEqualTo(preview)
    }

    private fun assertFinished(f: Fixture) {
        assertThat(f.engine.state.value.phase).isEqualTo(SessionState.Phase.Idle)
        assertThat(f.engine.state.value.blockerArmed).isFalse()
        assertThat(f.engine.state.value.remaining).isNull()
        assertThat(f.engine.state.value.plannedSets).isNull()
        assertThat(f.repository.activeWorkoutId.value).isNull()
        assertThat(f.alarm.deadline).isNull()
    }

    private suspend fun TestScope.fixture(
        plan: PlannedWorkout = workout(),
        file: File = File(temporaryFolder.newFolder(), "session.preferences_pb"),
        repository: MemoryFitnessRepository = MemoryFitnessRepository(),
    ): Fixture {
        if (repository.savedWorkouts.value.isEmpty()) repository.saveWorkout(plan)
        val sessionJob = SupervisorJob(backgroundScope.coroutineContext[Job])
        val storageJob = SupervisorJob(backgroundScope.coroutineContext[Job])
        val sessionScope = CoroutineScope(backgroundScope.coroutineContext + sessionJob)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(backgroundScope.coroutineContext + storageJob),
            produceFile = { file },
        )
        val store = WorkoutPreferencesStore(dataStore)
        val alarm = RecordingAlarm()
        val permissions = TestPermissions()
        val controller = DefaultWorkoutController(
            store, alarm, permissions, StaticAppBlockPolicy(emptySet()), sessionScope,
            object : Clock { override fun nowMillis() = testScheduler.currentTime },
        )
        val engine = BackendSessionEngine(controller, repository, sessionScope)
        val home = HomeViewModel(engine, FakeSettingsRepository(), repository, permissions, false)
        return Fixture(repository, engine, home, BlockerViewModel(engine, repository), alarm,
            store, file, sessionJob, storageJob)
    }

    private fun workout(sets: Int = 2, secondExercise: Boolean = true) = PlannedWorkout(
        id = "legs", name = "Leg day", createdAtMillis = 1L,
        exercises = buildList {
            add(PlannedExercise("barbell_squat", sets = sets, reps = 8, rank = 1))
            if (secondExercise) add(PlannedExercise("leg_press", sets = 1, reps = 12, rank = 2))
        },
    )

    private data class Fixture(
        val repository: MemoryFitnessRepository, val engine: BackendSessionEngine,
        val home: HomeViewModel, val blocker: BlockerViewModel, val alarm: RecordingAlarm,
        val store: WorkoutPreferencesStore, val file: File, val sessionJob: Job, val storageJob: Job,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    val dispatcher = UnconfinedTestDispatcher()
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}

private class RecordingAlarm : RestAlarmScheduler {
    var deadline: Long? = null
    override fun scheduleRestEnd(triggerAtMillis: Long) { deadline = triggerAtMillis }
    override fun cancelRestEnd() { deadline = null }
}

private class MemoryFitnessRepository : FitnessRepository {
    var logDelayMillis = 0L
    var failLogging = false
    override val userProfile = MutableStateFlow(UserProfile())
    override val savedWorkouts = MutableStateFlow<List<PlannedWorkout>>(emptyList())
    override val workoutLogs = MutableStateFlow<List<WorkoutLog>>(emptyList())
    override val activeWorkoutId = MutableStateFlow<String?>(null)
    override suspend fun saveUserProfile(profile: UserProfile) { userProfile.value = profile }
    override suspend fun saveWorkout(workout: PlannedWorkout) {
        savedWorkouts.value = listOf(workout) + savedWorkouts.value.filterNot { it.id == workout.id }
    }
    override suspend fun setActiveWorkoutId(workoutId: String?) { activeWorkoutId.value = workoutId }
    override suspend fun logWorkout(log: WorkoutLog) { workoutLogs.value = listOf(log) + workoutLogs.value }
    override suspend fun logActiveWorkoutAndClear(completedAtMillis: Long): Boolean {
        delay(logDelayMillis)
        if (failLogging) throw java.io.IOException("Storage unavailable")
        val workout = savedWorkouts.value.firstOrNull { it.id == activeWorkoutId.value }
        activeWorkoutId.value = null
        if (workout == null) return false
        logWorkout(WorkoutLog(workout.name, completedAtMillis, 1, 0, workout.exercises.size))
        return true
    }
}

private class TestPermissions : PermissionGateway {
    override fun currentStatus() = PermissionStatus(isAccessibilityServiceEnabled = true)
    override fun accessibilitySettingsIntent(): Intent = error("Unused in session tests")
    override fun appDetailsSettingsIntent(): Intent = error("Unused in session tests")
}
