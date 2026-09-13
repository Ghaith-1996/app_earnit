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
