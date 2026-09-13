package com.restlock.ui

import android.content.Intent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.fitness.restlock.backend.PermissionStatus
import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutController
import com.fitness.restlock.backend.alarm.RestAlarmScheduler
import com.fitness.restlock.backend.blocking.StaticAppBlockPolicy
import com.fitness.restlock.backend.permissions.PermissionGateway
import com.fitness.restlock.backend.persistence.WorkoutPreferencesStore
import com.fitness.restlock.backend.session.Clock
import com.fitness.restlock.backend.session.DefaultWorkoutController
import com.google.common.truth.Truth.assertThat
import com.restlock.domain.*
import com.restlock.domain.backend.BackendSessionEngine
import com.restlock.domain.backend.BackendFitnessRepository
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
    fun `delete and edit cannot race a saved workout start before its metadata write`() = runTest {
        val scope = backgroundScope
        val sessionStore = WorkoutPreferencesStore(PreferenceDataStoreFactory.create(scope = scope) {
            File(temporaryFolder.newFolder(), "session.preferences_pb")
        })
        val fitnessStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(temporaryFolder.newFolder(), "fitness.preferences_pb")
        }
        val repository = BackendFitnessRepository(fitnessStore)
        val secondRepository = BackendFitnessRepository(fitnessStore)
        repository.saveWorkout(workout())
        val actualController = DefaultWorkoutController(
            sessionStore, RecordingAlarm(), TestPermissions(), StaticAppBlockPolicy(emptySet()), scope,
            object : Clock { override fun nowMillis() = testScheduler.currentTime },
        )
        val releaseStart = CompletableDeferred<Unit>()
        val controller = object : WorkoutController by actualController {
            override suspend fun startWorkout(restSeconds: Int, plannedSets: Int?, workoutId: String?): Boolean {
                val started = actualController.startWorkout(restSeconds, plannedSets, workoutId)
                releaseStart.await()
                return started
            }
        }
        val engine = BackendSessionEngine(controller, repository, scope)
        engine.startWorkout(5.seconds, workout())
        runCurrent()
        assertThat(actualController.activeWorkout()?.workoutId).isEqualTo("legs")
        assertThat(repository.activeWorkoutId.first()).isNull()
        val deletion = async { secondRepository.deleteWorkout("legs") }
        val edit = async { secondRepository.saveWorkout(workout().copy(name = "Changed"), requireExisting = true) }
        runCurrent()
        assertThat(deletion.isCompleted).isFalse()
        assertThat(edit.isCompleted).isFalse()
        releaseStart.complete(Unit)
        runCurrent()
        assertThat(deletion.await()).isEqualTo(WorkoutMutationResult.ActiveWorkout)
        assertThat(edit.await()).isEqualTo(WorkoutMutationResult.ActiveWorkout)
        assertThat(repository.savedWorkouts.first()).containsExactly(workout())
        assertThat(repository.activeWorkoutId.first()).isEqualTo("legs")
        engine.finishWorkout()
        runCurrent()
        assertThat(repository.workoutLogs.first().single().plannedSets).isEqualTo(3)
        assertThat(secondRepository.deleteWorkout("legs")).isEqualTo(WorkoutMutationResult.Success)
        assertThat(repository.workoutLogs.first()).hasSize(1)
    }

    @Test
    fun `process death between backend start and fitness metadata write restores original identity`() = runTest {
        val f = fixture()
        runCurrent()
        advanceTimeBy(1_000)
        f.controller.startWorkout(5, 3, "legs")
        assertThat(f.repository.activeWorkoutSession.value).isNull()
        f.sessionJob.cancelAndJoin()
        f.storageJob.cancelAndJoin()
        advanceTimeBy(60_000)
        val restored = fixture(file = f.file, repository = f.repository)
        runCurrent()
        assertThat(restored.repository.activeWorkoutSession.value).isEqualTo(ActiveWorkoutSession("legs", 1_000L))
        restored.home.finishWorkout()
        runCurrent()
        assertThat(restored.repository.workoutLogs.value.single().durationMinutes).isEqualTo(1)
    }

    @Test
    fun `both stores reopen mid workout and retain original start for a 42 minute result`() = runTest {
        val sessionFile = File(temporaryFolder.newFolder(), "session.preferences_pb")
        val fitnessFile = File(temporaryFolder.newFolder(), "fitness.preferences_pb")
        val jobs = mutableListOf<Job>()
        fun open(): Pair<BackendSessionEngine, BackendFitnessRepository> {
            val job = SupervisorJob(backgroundScope.coroutineContext[Job]).also(jobs::add)
            val scope = CoroutineScope(backgroundScope.coroutineContext + job)
            val store = WorkoutPreferencesStore(PreferenceDataStoreFactory.create(scope = scope) { sessionFile })
            val repository = BackendFitnessRepository(PreferenceDataStoreFactory.create(scope = scope) { fitnessFile })
            val controller = DefaultWorkoutController(
                store, RecordingAlarm(), TestPermissions(), StaticAppBlockPolicy(emptySet()), scope,
                object : Clock { override fun nowMillis() = testScheduler.currentTime },
            )
            return BackendSessionEngine(controller, repository, scope) to repository
        }
        advanceTimeBy(1_000)
        val (engine, repository) = open()
        repository.saveWorkout(workout())
        engine.startWorkout(5.seconds, workout())
        runCurrent()
        repeat(2) {
            advanceTimeBy(5_000)
            runCurrent()
            engine.exerciseDone()
            runCurrent()
        }
        jobs.last().cancelAndJoin()
        advanceTimeBy(2_510_000)
        val (restoredEngine, restoredRepository) = open()
        runCurrent()
        assertThat(restoredEngine.state.value.setsCompleted).isEqualTo(2)
        assertThat(restoredEngine.state.value.isFinalSet).isTrue()
        assertThat(restoredRepository.activeWorkoutSession.first()?.startedAtMillis).isEqualTo(1_000L)
        var delivered: WorkoutLog? = null
        restoredEngine.exerciseDone { delivered = it }
        runCurrent()
        val log = restoredRepository.workoutLogs.first().single()
        assertThat(log).isEqualTo(delivered)
        assertThat(restoredRepository.pendingWorkoutSummary.first()).isEqualTo(log)
        assertThat(log.startedAtMillis).isEqualTo(1_000L)
        assertThat(log.completedAtMillis).isEqualTo(2_521_000L)
        assertThat(log.durationMinutes).isEqualTo(42)
        assertThat(log.completedSets).isEqualTo(3)
        assertThat(log.completedFully).isTrue()
    }

    @Test
    fun `manual finish returns persisted partial result before counters are reset`() = runTest {
        val f = fixture()
        f.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        f.home.exerciseDone()
        runCurrent()
        var result: WorkoutLog? = null
        f.blocker.finishWorkout { result = it }
        f.home.finishWorkout()
        runCurrent()
        assertFinished(f)
        assertThat(result).isEqualTo(f.repository.workoutLogs.value.single())
        assertThat(result!!.completedSets).isEqualTo(1)
        assertThat(result!!.plannedSets).isEqualTo(3)
        assertThat(result!!.completedFully).isFalse()
        assertThat(result!!.startedAtMillis).isEqualTo(0L)
        assertThat(result!!.completedAtMillis).isEqualTo(5_000L)
    }

    @Test
    fun `final set callback includes the final set and the persisted summary`() = runTest {
        val f = fixture(workout(sets = 1, secondExercise = false))
        f.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        var result: WorkoutLog? = null
        f.blocker.exerciseDone { result = it }
        runCurrent()
        assertThat(result!!.completedSets).isEqualTo(1)
        assertThat(result!!.completedFully).isTrue()
        assertThat(f.repository.pendingWorkoutSummary.value).isEqualTo(result)
        assertFinished(f)
    }

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
        repository.saveWorkout(workout())
        val vm = WorkoutViewModel(repository)
        vm.startEditingWorkout(workout())
        runCurrent()
        vm.moveExerciseDown("barbell_squat")
        vm.saveWorkout()
        runCurrent()
        assertThat(repository.savedWorkouts.value.single().exerciseIds)
            .containsExactly("leg_press", "barbell_squat").inOrder()
        vm.startEditingWorkout(repository.savedWorkouts.value.single())
        runCurrent()
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
    fun `logging failure unlocks and retains a recoverable completion after process death`() = runTest {
        val f = fixture(workout(sets = 1, secondExercise = false))
        f.repository.failLogging = true
        f.home.startSavedWorkout("legs", 5.seconds)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        f.blocker.exerciseDone()
        runCurrent()
        assertThat(f.engine.state.value.phase).isEqualTo(SessionState.Phase.Idle)
        assertThat(f.engine.state.value.blockerArmed).isFalse()
        assertThat(f.alarm.deadline).isNull()
        assertThat(f.store.snapshots.first().pendingCompletion!!.completedSets).isEqualTo(1)
        f.sessionJob.cancelAndJoin()
        f.storageJob.cancelAndJoin()
        advanceTimeBy(60_000)
        f.repository.failLogging = false
        val restored = fixture(file = f.file, repository = f.repository)
        runCurrent()
        assertFinished(restored)
        assertThat(f.repository.workoutLogs.value.single().completedAtMillis).isEqualTo(5_000L)
        assertThat(f.repository.workoutLogs.value.single().completedSets).isEqualTo(1)
        restored.home.startWorkout(5.seconds)
        runCurrent()
        assertThat(restored.engine.state.value.phase).isEqualTo(SessionState.Phase.Resting)
    }

    @Test
    fun `finish clears a missing active workout without logging`() = runTest {
        val f = fixture()
        f.repository.setActiveWorkoutSession(ActiveWorkoutSession("deleted", null))
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
            store, file, sessionJob, storageJob, controller)
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
        val controller: DefaultWorkoutController,
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
    override val activeWorkoutSession = MutableStateFlow<ActiveWorkoutSession?>(null)
    override val pendingWorkoutSummary = MutableStateFlow<WorkoutLog?>(null)
    override suspend fun saveUserProfile(profile: UserProfile) { userProfile.value = profile }
    override suspend fun saveWorkout(workout: PlannedWorkout, requireExisting: Boolean): WorkoutMutationResult {
        savedWorkouts.value = listOf(workout) + savedWorkouts.value.filterNot { it.id == workout.id }
        return WorkoutMutationResult.Success
    }
    override suspend fun deleteWorkout(workoutId: String): WorkoutMutationResult {
        if (activeWorkoutId.value == workoutId) return WorkoutMutationResult.ActiveWorkout
        savedWorkouts.value = savedWorkouts.value.filterNot { it.id == workoutId }
        return WorkoutMutationResult.Success
    }
    override suspend fun setActiveWorkoutSession(session: ActiveWorkoutSession?) {
        activeWorkoutSession.value = session
        activeWorkoutId.value = session?.workoutId
    }
    override suspend fun dismissWorkoutSummary() { pendingWorkoutSummary.value = null }
    override suspend fun logWorkout(log: WorkoutLog) { workoutLogs.value = listOf(log) + workoutLogs.value }
    override suspend fun logActiveWorkoutAndClear(completedAtMillis: Long, completedSets: Int): WorkoutLog? {
        delay(logDelayMillis)
        if (failLogging) throw java.io.IOException("Storage unavailable")
        val workout = savedWorkouts.value.firstOrNull { it.id == activeWorkoutId.value }
        val session = activeWorkoutSession.value
        setActiveWorkoutSession(null)
        if (workout == null || session == null) return null
        val log = WorkoutResults.createLog(workout, session, completedSets, completedAtMillis, userProfile.value)
        logWorkout(log)
        pendingWorkoutSummary.value = log
        return log
    }
}

private class TestPermissions : PermissionGateway {
    override fun currentStatus() = PermissionStatus(isAccessibilityServiceEnabled = true)
    override fun accessibilitySettingsIntent(): Intent = error("Unused in session tests")
    override fun appDetailsSettingsIntent(): Intent = error("Unused in session tests")
}
