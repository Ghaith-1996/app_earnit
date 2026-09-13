package com.restlock.domain.backend

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.restlock.domain.ActiveWorkoutSession
import com.restlock.domain.PlannedExercise
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.UserProfile
import com.restlock.domain.WorkoutLog
import com.restlock.domain.WorkoutMutationResult
import com.restlock.domain.normalizedWorkoutExercises
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackendFitnessRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val jobs = mutableListOf<Job>()
    private val workout = PlannedWorkout(
        "push", "Push + Day | é", listOf(PlannedExercise("bench_press", sets = 4), PlannedExercise("leg_press", sets = 3, rank = 2)), 1L,
    )

    private fun openStore(): DataStore<Preferences> {
        val job = SupervisorJob().also(jobs::add)
        return PreferenceDataStoreFactory.create(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { temporaryFolder.root.resolve("fitness.preferences_pb") },
        )
    }

    @After fun tearDown() = runBlocking { jobs.forEach { it.cancelAndJoin() } }

    @Test
    fun `deleting one routine leaves other routines history and pending summary intact`() = runBlocking<Unit> {
        val repository = BackendFitnessRepository(openStore())
        val pull = workout.copy(id = "pull", name = "Pull Day", createdAtMillis = 2L)
        val legs = workout.copy(id = "legs", name = "Leg Day", createdAtMillis = 3L)
        listOf(workout, pull, legs).forEach { repository.saveWorkout(it) }
        repository.setActiveWorkoutSession(ActiveWorkoutSession(workout.id, 1_000L))
        val history = repository.logActiveWorkoutAndClear(61_000L, 3)!!

        assertThat(repository.deleteWorkout(workout.id)).isEqualTo(WorkoutMutationResult.Success)
        assertThat(repository.savedWorkouts.first()).containsExactly(legs, pull).inOrder()
        assertThat(repository.workoutLogs.first()).containsExactly(history)
        assertThat(repository.pendingWorkoutSummary.first()).isEqualTo(history)
    }

    @Test
    fun `unknown deletion and stale edit leave preferences byte for byte unchanged`() = runBlocking<Unit> {
        val store = openStore()
        val repository = BackendFitnessRepository(store)
        repository.saveWorkout(workout)
        store.edit { it[stringPreferencesKey("saved_workouts")] += "\nmalformed retained row" }
        val before = store.data.first().asMap()

        assertThat(repository.deleteWorkout("missing")).isEqualTo(WorkoutMutationResult.NotFound)
        assertThat(repository.saveWorkout(workout.copy(id = "missing"), requireExisting = true))
            .isEqualTo(WorkoutMutationResult.NotFound)
        assertThat(store.data.first().asMap()).isEqualTo(before)
    }

    @Test
    fun `active routine cannot be deleted or edited and its session remains coherent`() = runBlocking<Unit> {
        val store = openStore()
        val repository = BackendFitnessRepository(store)
        repository.saveWorkout(workout)
        repository.setActiveWorkoutSession(ActiveWorkoutSession(workout.id, 42L))
        val before = store.data.first().asMap()

        assertThat(repository.deleteWorkout(workout.id)).isEqualTo(WorkoutMutationResult.ActiveWorkout)
        assertThat(repository.saveWorkout(workout.copy(name = "Changed", exercises = listOf(PlannedExercise("leg_press")))))
            .isEqualTo(WorkoutMutationResult.ActiveWorkout)
        assertThat(store.data.first().asMap()).isEqualTo(before)
        repository.setActiveWorkoutSession(null)
        assertThat(repository.deleteWorkout(workout.id)).isEqualTo(WorkoutMutationResult.Success)
    }

    @Test
    fun `backend active identity protects routine before fitness metadata is restored`() = runBlocking<Unit> {
        val store = openStore()
        BackendFitnessRepository(store).saveWorkout(workout)
        val repository = BackendFitnessRepository(store) { workout.id }
        val before = store.data.first().asMap()
        assertThat(repository.activeWorkoutId.first()).isNull()
        assertThat(repository.deleteWorkout(workout.id)).isEqualTo(WorkoutMutationResult.ActiveWorkout)
        assertThat(repository.saveWorkout(workout.copy(name = "Changed"))).isEqualTo(WorkoutMutationResult.ActiveWorkout)
        assertThat(store.data.first().asMap()).isEqualTo(before)
    }

    @Test
    fun `session selection lock prevents another repository deleting selected routine before activation`() = runBlocking<Unit> {
        val store = openStore()
        val repository = BackendFitnessRepository(store)
        repository.saveWorkout(workout)
        val selected = CompletableDeferred<Unit>()
        val canActivate = CompletableDeferred<Unit>()
        val starting = async(Dispatchers.Default) {
            repository.withWorkoutStartLock {
                assertThat(repository.savedWorkouts.first()).contains(workout)
                selected.complete(Unit)
                canActivate.await()
                repository.setActiveWorkoutSession(ActiveWorkoutSession(workout.id, 42L))
            }
        }
        selected.await()
        val deletionAttempted = CompletableDeferred<Unit>()
        val deleting = async(Dispatchers.Default) {
            deletionAttempted.complete(Unit)
            BackendFitnessRepository(store).deleteWorkout(workout.id)
        }
        deletionAttempted.await()
        assertThat(deleting.isCompleted).isFalse()
        canActivate.complete(Unit)
        starting.await()
        assertThat(deleting.await()).isEqualTo(WorkoutMutationResult.ActiveWorkout)
        assertThat(repository.savedWorkouts.first()).containsExactly(workout)
    }

    @Test
    fun `invalid routines never write a datastore record`() = runBlocking<Unit> {
        val store = openStore()
        val repository = BackendFitnessRepository(store)
        val before = store.data.first().asMap()
        listOf(
            workout.copy(id = "  "),
            workout.copy(exercises = emptyList()),
            workout.copy(exercises = listOf(PlannedExercise("unknown_exercise"))),
        ).forEach { assertThat(repository.saveWorkout(it)).isEqualTo(WorkoutMutationResult.InvalidWorkout) }
        assertThat(store.data.first().asMap()).isEqualTo(before)
    }

    @Test
    fun `save normalizes name duplicates limits and ranks and preserves original creation time on edit`() = runBlocking<Unit> {
        val repository = BackendFitnessRepository(openStore())
        val invalid = workout.copy(
            name = "    ",
            exercises = listOf(
                PlannedExercise("bench_press", sets = 999, reps = 5000, rank = 5),
                PlannedExercise("leg_press", sets = -5, reps = 0, rank = 1),
                PlannedExercise("bench_press", sets = 2, reps = 2, rank = 5),
                PlannedExercise("unknown", rank = -1),
                PlannedExercise("barbell_squat", sets = 0, reps = -1, rank = 100),
            ),
        )
        assertThat(repository.saveWorkout(invalid)).isEqualTo(WorkoutMutationResult.Success)
        val saved = repository.savedWorkouts.first().single()
        assertThat(saved.name).isEqualTo("Workout")
        assertThat(saved.exercises).containsExactly(
            PlannedExercise("leg_press", PlannedExercise.MinSets, PlannedExercise.MinReps, 1),
            PlannedExercise("bench_press", PlannedExercise.MaxSets, PlannedExercise.MaxReps, 2),
            PlannedExercise("barbell_squat", PlannedExercise.MinSets, PlannedExercise.MinReps, 3),
        ).inOrder()
        assertThat(repository.saveWorkout(saved.copy(name = "  Push Day  ", createdAtMillis = 9_999L), requireExisting = true))
            .isEqualTo(WorkoutMutationResult.Success)
        assertThat(repository.savedWorkouts.first()).containsExactly(saved.copy(name = "Push Day"))
    }

    @Test
    fun `current workout roundtrip preserves encoded characters sets reps and deterministic rank order`() = runBlocking<Unit> {
        val repository = BackendFitnessRepository(openStore())
        val original = workout.copy(id = "routine | + , : é\n%", name = "Force | + , : é\n%", exercises = listOf(
            PlannedExercise("bench_press", 4, 8, 5),
            PlannedExercise("leg_press", 2, 12, 1),
            PlannedExercise("barbell_squat", 3, 15, 100),
        ))
        repository.saveWorkout(original)
        jobs.last().cancelAndJoin()
        assertThat(BackendFitnessRepository(openStore()).savedWorkouts.first())
            .containsExactly(original.copy(exercises = original.exercises.normalizedWorkoutExercises()))
    }

    @Test
    fun `malformed rows and exercises are isolated while valid legacy and current routines survive`() = runBlocking<Unit> {
        val store = openStore()
        store.edit {
            it[stringPreferencesKey("saved_workouts")] = listOf(
                "workout-v1|legacy|1|%20Old+Day%20|bench_press,missing,leg_press,bench_press",
                "unparseable",
                "workout-v2|bad_date|oops|Bad|bench_press:3:10:1",
                "workout-v2|bad_escape|3|Bad%XX|bench_press:3:10:1",
                "workout-v2||3|No%20ID|bench_press:3:10:1",
                "workout-v2|empty|3|Empty|unknown:3:10:1",
                "workout-v3|future|3|Future|bench_press:3:10:1",
                "workout-v2|current|2|%20Push%20Day%20|bench_press:999:0:5,%XX:1:1:2,leg_press:-5:5000:1,missing:3:10:3,bench_press:3:10:100,barbell_squat:oops:oops:oops",
            ).joinToString("\n")
        }
        val repository = BackendFitnessRepository(store)
        val loaded = repository.savedWorkouts.first()
        assertThat(loaded.map { it.id }).containsExactly("current", "legacy").inOrder()
        assertThat(loaded[1].name).isEqualTo("Old+Day")
        assertThat(loaded[1].exercises).containsExactly(PlannedExercise("bench_press"), PlannedExercise("leg_press", rank = 2)).inOrder()
        assertThat(loaded[0].exercises).containsExactly(
            PlannedExercise("leg_press", PlannedExercise.MinSets, PlannedExercise.MaxReps, 1),
            PlannedExercise("bench_press", PlannedExercise.MaxSets, PlannedExercise.MinReps, 2),
            PlannedExercise("barbell_squat", rank = 3),
        ).inOrder()
        repository.saveWorkout(loaded[1])
        jobs.last().cancelAndJoin()
        assertThat(BackendFitnessRepository(openStore()).savedWorkouts.first()).containsExactlyElementsIn(loaded).inOrder()
    }

    @Test
    fun `malformed logs are skipped per row and legacy plus current logs survive rewrite`() = runBlocking<Unit> {
        val store = openStore()
        store.edit {
            it[stringPreferencesKey("workout_logs")] = listOf(
                "log-v1|400|Old+Name%20%C3%A9|25|120|2",
                "log-v2|500|Partial||||||",
                "log-v1|600|Bad%ZZ|2|30|1",
                "log-v2|700|Bad%20start|1|30|1|oops|1|3",
                "log-v2|800|Good%20partial||30|1||1|3",
                "log-v3|900|Unsupported|1|30|1|1|1|3",
            ).joinToString("\n")
        }
        val repository = BackendFitnessRepository(store)
        val legacy = WorkoutLog("Old+Name é", 400L, 25, 120, 2)
        val current = WorkoutLog("Good partial", 800L, null, 30, 1, completedSets = 1, plannedSets = 3)
        assertThat(repository.workoutLogs.first()).containsExactly(current, legacy).inOrder()
        val newest = WorkoutLog("New | + é", 1_000L, 10, 50, 1, 900L, 3, 3)
        repository.logWorkout(newest)
        jobs.last().cancelAndJoin()
        assertThat(BackendFitnessRepository(openStore()).workoutLogs.first()).containsExactly(newest, current, legacy).inOrder()
    }

    @Test
    fun `saved workout limit evicts oldest inactive routine and retains both active identities`() = runBlocking<Unit> {
        val store = openStore()
        val setup = BackendFitnessRepository(store)
        repeat(30) { index -> setup.saveWorkout(workout.copy(id = "routine-$index", createdAtMillis = index.toLong())) }
        setup.setActiveWorkoutSession(ActiveWorkoutSession("routine-0", 1L))
        val repository = BackendFitnessRepository(store) { "routine-1" }
        assertThat(repository.saveWorkout(workout.copy(id = "new", createdAtMillis = 100L)))
            .isEqualTo(WorkoutMutationResult.Success)
        val ids = repository.savedWorkouts.first().map { it.id }
        assertThat(ids).hasSize(30)
        assertThat(ids).containsAtLeast("routine-0", "routine-1", "new")
        assertThat(ids).doesNotContain("routine-2")
        assertThat(repository.saveWorkout(workout.copy(id = "routine-3", name = "Updated", createdAtMillis = 200L), requireExisting = true))
            .isEqualTo(WorkoutMutationResult.Success)
        assertThat(repository.savedWorkouts.first()).hasSize(30)
        assertThat(repository.savedWorkouts.first().single { it.id == "routine-3" }.createdAtMillis).isEqualTo(3L)
    }

    @Test
    fun `concurrent routine saves retain all independent additions`() = runBlocking<Unit> {
        val store = openStore()
        List(12) { index ->
            async(Dispatchers.Default) {
                BackendFitnessRepository(store).saveWorkout(workout.copy(id = "routine-$index", createdAtMillis = index.toLong()))
            }
        }.awaitAll().forEach { assertThat(it).isEqualTo(WorkoutMutationResult.Success) }
        assertThat(BackendFitnessRepository(store).savedWorkouts.first().map { it.id })
            .containsExactlyElementsIn((0 until 12).map { "routine-$it" })
    }

    @Test
    fun `history stays bounded at thirty newest records`() = runBlocking<Unit> {
        val repository = BackendFitnessRepository(openStore())
        repeat(31) { index -> repository.logWorkout(WorkoutLog("Log $index", index.toLong(), 1, 1, 1)) }
        assertThat(repository.workoutLogs.first()).hasSize(30)
        assertThat(repository.workoutLogs.first().map { it.completedAtMillis })
            .containsExactlyElementsIn((30L downTo 1L).toList()).inOrder()
    }

    @Test
    fun `start survives fresh repository and actual file reopen then summary equals persisted full log`() = runBlocking<Unit> {
        val store = openStore()
        val repository = BackendFitnessRepository(store)
        repository.saveWorkout(workout)
        repository.saveUserProfile(UserProfile(weightKg = 80.0))
        repository.setActiveWorkoutSession(ActiveWorkoutSession(workout.id, 1_000L))
        assertThat(BackendFitnessRepository(store).activeWorkoutSession.first()).isEqualTo(ActiveWorkoutSession(workout.id, 1_000L))
        jobs.last().cancelAndJoin()
        val restored = BackendFitnessRepository(openStore())
        assertThat(restored.activeWorkoutId.first()).isEqualTo(workout.id)
        assertThat(restored.activeWorkoutSession.first()?.startedAtMillis).isEqualTo(1_000L)
        val log = restored.logActiveWorkoutAndClear(2_521_000L, 7)!!
        assertThat(log.name).isEqualTo(workout.name)
        assertThat(log.durationMinutes).isEqualTo(42)
        assertThat(log.startedAtMillis).isEqualTo(1_000L)
        assertThat(log.completedAtMillis).isEqualTo(2_521_000L)
        assertThat(log.completedSets).isEqualTo(7)
        assertThat(log.plannedSets).isEqualTo(7)
        assertThat(log.completedFully).isTrue()
        assertThat(log.calories).isGreaterThan(0)
        assertThat(log.exerciseCount).isEqualTo(2)
        assertThat(restored.activeWorkoutSession.first()).isNull()
        assertThat(restored.pendingWorkoutSummary.first()).isEqualTo(log)
        jobs.last().cancelAndJoin()
        val reopened = BackendFitnessRepository(openStore())
        assertThat(reopened.workoutLogs.first()).containsExactly(log)
        assertThat(reopened.pendingWorkoutSummary.first()).isEqualTo(log)
        reopened.dismissWorkoutSummary()
        assertThat(reopened.pendingWorkoutSummary.first()).isNull()
        assertThat(reopened.workoutLogs.first()).containsExactly(log)
        jobs.last().cancelAndJoin()
        assertThat(BackendFitnessRepository(openStore()).pendingWorkoutSummary.first()).isNull()
    }

    @Test
    fun `concurrent finishes write one partial log and retain the same pending result`() = runBlocking<Unit> {
        val store = openStore()
        val repository = BackendFitnessRepository(store)
        repository.saveWorkout(workout)
        repository.setActiveWorkoutSession(ActiveWorkoutSession(workout.id, 1_000L))
        val results = List(12) { async(Dispatchers.Default) { BackendFitnessRepository(store).logActiveWorkoutAndClear(61_000L, 3) } }.awaitAll()
        val log = results.filterNotNull().single()
        assertThat(log.completedSets).isEqualTo(3)
        assertThat(log.plannedSets).isEqualTo(7)
        assertThat(log.completedFully).isFalse()
        assertThat(log.exerciseCount).isEqualTo(1)
        assertThat(repository.workoutLogs.first()).containsExactly(log)
        assertThat(repository.pendingWorkoutSummary.first()).isEqualTo(log)
        assertThat(repository.activeWorkoutSession.first()).isNull()
    }

    @Test
    fun `legacy logs and workouts stay readable through v2 rewrite and missing start stays unknown`() = runBlocking<Unit> {
        val store = openStore()
        store.edit {
            it[stringPreferencesKey("workout_logs")] = "log-v1|400|Old+Name%20%C3%A9|25|120|2"
            it[stringPreferencesKey("saved_workouts")] = "workout-v1|push|1|Push%20Day|bench_press,leg_press"
            it[stringPreferencesKey("active_workout_id")] = "push"
        }
        val repository = BackendFitnessRepository(store)
        val legacy = WorkoutLog("Old+Name é", 400L, 25, 120, 2)
        assertThat(repository.workoutLogs.first()).containsExactly(legacy)
        assertThat(repository.activeWorkoutSession.first()).isEqualTo(ActiveWorkoutSession("push", null))
        assertThat(repository.savedWorkouts.first().single().totalSets).isEqualTo(6)
        val result = repository.logActiveWorkoutAndClear(61_000L, 1)!!
        assertThat(result.startedAtMillis).isNull()
        assertThat(result.durationMinutes).isNull()
        assertThat(result.completedFully).isFalse()
        assertThat(repository.workoutLogs.first()).containsExactly(result, legacy).inOrder()
        jobs.last().cancelAndJoin()
        val restored = BackendFitnessRepository(openStore())
        assertThat(restored.workoutLogs.first()).containsExactly(result, legacy).inOrder()
        assertThat(restored.savedWorkouts.first().single().name).isEqualTo("Push Day")
    }

    @Test
    fun `missing workout and Quick Start clear all active metadata without producing logs`() = runBlocking<Unit> {
        val store = openStore()
        val repository = BackendFitnessRepository(store)
        repository.setActiveWorkoutSession(ActiveWorkoutSession("missing", 1_000L))
        assertThat(repository.logActiveWorkoutAndClear(61_000L, 3)).isNull()
        assertThat(repository.activeWorkoutSession.first()).isNull()
        assertThat(store.data.first()[longPreferencesKey("active_workout_started_at_millis")]).isNull()
        repository.saveWorkout(workout.copy(exercises = emptyList()))
        repository.setActiveWorkoutSession(ActiveWorkoutSession(workout.id, 1_000L))
        assertThat(repository.logActiveWorkoutAndClear(61_000L, 0)).isNull()
        assertThat(repository.activeWorkoutSession.first()).isNull()
        assertThat(repository.workoutLogs.first()).isEmpty()
        repository.setActiveWorkoutSession(ActiveWorkoutSession("push", 2_000L))
        repository.setActiveWorkoutSession(null)
        assertThat(repository.logActiveWorkoutAndClear(61_000L, 10)).isNull()
        assertThat(repository.workoutLogs.first()).isEmpty()
        assertThat(repository.pendingWorkoutSummary.first()).isNull()
        assertThat(store.data.first()[longPreferencesKey("active_workout_started_at_millis")]).isNull()
        store.edit { it[longPreferencesKey("active_workout_started_at_millis")] = 123L }
        assertThat(repository.logActiveWorkoutAndClear(61_000L, 10)).isNull()
        assertThat(store.data.first()[longPreferencesKey("active_workout_started_at_millis")]).isNull()
    }
}
