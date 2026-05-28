package com.restlock.domain.backend

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.restlock.domain.FitnessCalculator
import com.restlock.domain.FitnessRepository
import com.restlock.domain.PlannedExercise
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.UserProfile
import com.restlock.domain.UserSex
import com.restlock.domain.WorkoutLog
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.fitnessDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "fitness_repository",
)

class BackendFitnessRepository(context: Context) : FitnessRepository {
    private val dataStore = context.applicationContext.fitnessDataStore

    private val safeData: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    override val userProfile: Flow<UserProfile> = safeData.map { preferences ->
        preferences.toUserProfile()
    }

    override val savedWorkouts: Flow<List<PlannedWorkout>> = safeData.map { preferences ->
        decodeWorkouts(preferences[Keys.savedWorkouts].orEmpty())
    }

    override val workoutLogs: Flow<List<WorkoutLog>> = safeData.map { preferences ->
        decodeLogs(preferences[Keys.workoutLogs].orEmpty())
    }

    override val activeWorkoutId: Flow<String?> = safeData.map { preferences ->
        preferences[Keys.activeWorkoutId]
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        dataStore.edit { preferences ->
            val age = profile.ageYears
            val weight = profile.weightKg
            val height = profile.heightCm

            if (age == null) preferences.remove(Keys.ageYears) else preferences[Keys.ageYears] = age
            preferences[Keys.sex] = profile.sex.name
            if (weight == null) preferences.remove(Keys.weightKg) else preferences[Keys.weightKg] = weight
            if (height == null) preferences.remove(Keys.heightCm) else preferences[Keys.heightCm] = height
        }
    }

    override suspend fun saveWorkout(workout: PlannedWorkout) {
        dataStore.edit { preferences ->
            val current = decodeWorkouts(preferences[Keys.savedWorkouts].orEmpty())
            val next = listOf(workout) + current.filterNot { it.id == workout.id }
            preferences[Keys.savedWorkouts] = next
                .take(MaxSavedWorkouts)
                .joinToString(RecordSeparator, transform = ::encodeWorkout)
        }
    }

    override suspend fun logWorkout(log: WorkoutLog) {
        dataStore.edit { preferences ->
            val current = decodeLogs(preferences[Keys.workoutLogs].orEmpty())
            val next = listOf(log) + current
            preferences[Keys.workoutLogs] = next
                .take(MaxWorkoutLogs)
                .joinToString(RecordSeparator, transform = ::encodeLog)
        }
    }

    override suspend fun setActiveWorkoutId(workoutId: String?) {
        dataStore.edit { preferences ->
            if (workoutId.isNullOrBlank()) {
                preferences.remove(Keys.activeWorkoutId)
            } else {
                preferences[Keys.activeWorkoutId] = workoutId
            }
        }
    }

    override suspend fun logActiveWorkoutAndClear(completedAtMillis: Long): Boolean {
        var logged = false
        dataStore.edit { preferences ->
            val workoutId = preferences[Keys.activeWorkoutId]
            if (workoutId.isNullOrBlank()) {
                preferences.remove(Keys.activeWorkoutId)
                return@edit
            }

            val workout = decodeWorkouts(preferences[Keys.savedWorkouts].orEmpty())
                .firstOrNull { it.id == workoutId }
            if (workout == null) {
                preferences.remove(Keys.activeWorkoutId)
                return@edit
            }

            val log = WorkoutLog(
                name = workout.name,
                completedAtMillis = completedAtMillis,
                durationMinutes = FitnessCalculator.durationForPlannedExercises(workout.exercises),
                calories = FitnessCalculator.caloriesForPlannedExercises(
                    exercises = workout.exercises,
                    profile = preferences.toUserProfile(),
                ),
                exerciseCount = workout.exerciseIds.size,
            )
            val currentLogs = decodeLogs(preferences[Keys.workoutLogs].orEmpty())
            preferences[Keys.workoutLogs] = (listOf(log) + currentLogs)
                .take(MaxWorkoutLogs)
                .joinToString(RecordSeparator, transform = ::encodeLog)
            preferences.remove(Keys.activeWorkoutId)
            logged = true
        }
        return logged
    }

    private fun Preferences.toUserProfile(): UserProfile {
        val sexName = this[Keys.sex] ?: UserSex.Unspecified.name
        return UserProfile(
            ageYears = this[Keys.ageYears],
            sex = runCatching { UserSex.valueOf(sexName) }.getOrDefault(UserSex.Unspecified),
            weightKg = this[Keys.weightKg],
            heightCm = this[Keys.heightCm],
        )
    }

    private fun decodeWorkouts(raw: String): List<PlannedWorkout> {
        return raw.lineSequence()
            .mapNotNull(::decodeWorkout)
            .sortedByDescending { it.createdAtMillis }
            .toList()
    }

    private fun decodeLogs(raw: String): List<WorkoutLog> {
        return raw.lineSequence()
            .mapNotNull(::decodeLog)
            .sortedByDescending { it.completedAtMillis }
            .toList()
    }

    private fun encodeWorkout(workout: PlannedWorkout): String {
        return listOf(
            WorkoutVersion,
            encode(workout.id),
            workout.createdAtMillis.toString(),
            encode(workout.name),
            workout.orderedExercises.joinToString(ExerciseSeparator, transform = ::encodePlannedExercise),
        ).joinToString(FieldSeparator)
    }

    private fun decodeWorkout(raw: String): PlannedWorkout? {
        val parts = raw.split(FieldSeparator, limit = 5)
        if (parts.size != 5) return null
        val createdAt = parts[2].toLongOrNull() ?: return null
        val exercises = when (parts[0]) {
            WorkoutVersion -> decodePlannedExercises(parts[4])
            LegacyWorkoutVersion -> parts[4]
                .split(ExerciseSeparator)
                .filter { it.isNotBlank() }
                .mapIndexed { index, exerciseId ->
                    PlannedExercise(
                        exerciseId = exerciseId,
                        rank = index + 1,
                    )
                }
            else -> return null
        }
        return PlannedWorkout(
            id = decode(parts[1]),
            createdAtMillis = createdAt,
            name = decode(parts[3]).ifBlank { "Workout" },
            exercises = exercises,
        )
    }

    private fun encodePlannedExercise(exercise: PlannedExercise): String {
        return listOf(
            encode(exercise.exerciseId),
            exercise.sets.toString(),
            exercise.reps.toString(),
            exercise.rank.toString(),
        ).joinToString(ExerciseFieldSeparator)
    }

    private fun decodePlannedExercises(raw: String): List<PlannedExercise> {
        return raw.split(ExerciseSeparator)
            .filter { it.isNotBlank() }
            .mapIndexedNotNull { index, encodedExercise ->
                decodePlannedExercise(encodedExercise, fallbackRank = index + 1)
            }
    }

    private fun decodePlannedExercise(raw: String, fallbackRank: Int): PlannedExercise? {
        val parts = raw.split(ExerciseFieldSeparator, limit = 4)
        if (parts.isEmpty()) return null
        return PlannedExercise(
            exerciseId = decode(parts[0]).ifBlank { return null },
            sets = parts.getOrNull(1)?.toIntOrNull() ?: PlannedExercise.DefaultSets,
            reps = parts.getOrNull(2)?.toIntOrNull() ?: PlannedExercise.DefaultReps,
            rank = parts.getOrNull(3)?.toIntOrNull() ?: fallbackRank,
        )
    }

    private fun encodeLog(log: WorkoutLog): String {
        return listOf(
            LogVersion,
            log.completedAtMillis.toString(),
            encode(log.name),
            log.durationMinutes.toString(),
            log.calories.toString(),
            log.exerciseCount.toString(),
        ).joinToString(FieldSeparator)
    }

    private fun decodeLog(raw: String): WorkoutLog? {
        val parts = raw.split(FieldSeparator, limit = 6)
        if (parts.size != 6 || parts[0] != LogVersion) return null
        return WorkoutLog(
            completedAtMillis = parts[1].toLongOrNull() ?: return null,
            name = decode(parts[2]).ifBlank { "Workout" },
            durationMinutes = parts[3].toIntOrNull() ?: return null,
            calories = parts[4].toIntOrNull() ?: return null,
            exerciseCount = parts[5].toIntOrNull() ?: return null,
        )
    }

    private fun encode(value: String): String = Uri.encode(value)

    private fun decode(value: String): String = Uri.decode(value)

    private object Keys {
        val ageYears = intPreferencesKey("age_years")
        val sex = stringPreferencesKey("sex")
        val weightKg = doublePreferencesKey("weight_kg")
        val heightCm = intPreferencesKey("height_cm")
        val savedWorkouts = stringPreferencesKey("saved_workouts")
        val workoutLogs = stringPreferencesKey("workout_logs")
        val activeWorkoutId = stringPreferencesKey("active_workout_id")
    }

    private companion object {
        const val WorkoutVersion = "workout-v2"
        const val LegacyWorkoutVersion = "workout-v1"
        const val LogVersion = "log-v1"
        const val FieldSeparator = "|"
        const val ExerciseSeparator = ","
        const val ExerciseFieldSeparator = ":"
        const val RecordSeparator = "\n"
        const val MaxSavedWorkouts = 30
        const val MaxWorkoutLogs = 30
    }
}
