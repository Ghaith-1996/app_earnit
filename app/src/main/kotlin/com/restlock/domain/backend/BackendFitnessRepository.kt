package com.restlock.domain.backend

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.restlock.domain.ActiveWorkoutSession
import com.restlock.domain.FitnessRepository
import com.restlock.domain.PlannedExercise
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.UserProfile
import com.restlock.domain.UserSex
import com.restlock.domain.WorkoutLog
import com.restlock.domain.WorkoutResults
import com.restlock.domain.WorkoutMutationResult
import com.restlock.domain.normalizedWorkoutExercises
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.WeakHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.fitnessDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "fitness_repository",
)

class BackendFitnessRepository(
    private val dataStore: DataStore<Preferences>,
    private val backendActiveWorkoutId: suspend () -> String? = { null },
) : FitnessRepository {
    constructor(context: Context, backendActiveWorkoutId: suspend () -> String? = { null }) :
        this(context.applicationContext.fitnessDataStore, backendActiveWorkoutId)

    private val workoutMutationMutex = synchronized(workoutLocks) {
        workoutLocks.getOrPut(dataStore) { Mutex() }
    }

    override suspend fun <T> withWorkoutStartLock(action: suspend () -> T): T =
        workoutMutationMutex.withLock { action() }

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

    override val activeWorkoutSession: Flow<ActiveWorkoutSession?> = safeData.map { preferences ->
        preferences.toActiveWorkoutSession()
    }

    override val pendingWorkoutSummary: Flow<WorkoutLog?> = safeData.map { preferences ->
        preferences[Keys.pendingWorkoutSummary]?.let(::decodeLog)
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

    override suspend fun saveWorkout(workout: PlannedWorkout, requireExisting: Boolean): WorkoutMutationResult {
        val normalized = workout.copy(
            name = workout.name.trim().ifBlank { "Workout" },
            exercises = workout.exercises.normalizedWorkoutExercises(),
        )
        if (normalized.id.isBlank() || normalized.exercises.isEmpty()) return WorkoutMutationResult.InvalidWorkout
        return workoutMutationMutex.withLock {
            val backendActiveId = backendActiveWorkoutId()
            var result = WorkoutMutationResult.Success
            dataStore.edit { preferences ->
                val protectedIds = setOfNotNull(preferences[Keys.activeWorkoutId], backendActiveId)
                if (normalized.id in protectedIds) {
                    result = WorkoutMutationResult.ActiveWorkout
                    return@edit
                }
                val current = decodeWorkouts(preferences[Keys.savedWorkouts].orEmpty())
                val existing = current.firstOrNull { it.id == normalized.id }
                if (requireExisting && existing == null) {
                    result = WorkoutMutationResult.NotFound
                    return@edit
                }
                val saved = normalized.copy(createdAtMillis = existing?.createdAtMillis ?: normalized.createdAtMillis)
                val next = (listOf(saved) + current.filterNot { it.id == saved.id }).toMutableList()
                // Keep the current session's definition even when inserting at the storage limit.
                while (next.size > MaxSavedWorkouts) {
                    val removableIndex = next.indexOfLast { it.id !in protectedIds && it.id != saved.id }
                    next.removeAt(removableIndex)
                }
                preferences[Keys.savedWorkouts] = next.joinToString(RecordSeparator, transform = ::encodeWorkout)
            }
            result
        }
    }

    override suspend fun deleteWorkout(workoutId: String): WorkoutMutationResult = workoutMutationMutex.withLock {
        val backendActiveId = backendActiveWorkoutId()
        var result = WorkoutMutationResult.NotFound
        dataStore.edit { preferences ->
            if (workoutId == preferences[Keys.activeWorkoutId] || workoutId == backendActiveId) {
                result = WorkoutMutationResult.ActiveWorkout
                return@edit
            }
            val current = decodeWorkouts(preferences[Keys.savedWorkouts].orEmpty())
            if (current.none { it.id == workoutId }) return@edit
            preferences[Keys.savedWorkouts] = current.filterNot { it.id == workoutId }
                .joinToString(RecordSeparator, transform = ::encodeWorkout)
            result = WorkoutMutationResult.Success
        }
        result
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

    override suspend fun setActiveWorkoutSession(session: ActiveWorkoutSession?) {
        dataStore.edit { preferences ->
            if (session == null || session.workoutId.isBlank()) {
                preferences.remove(Keys.activeWorkoutId)
                preferences.remove(Keys.activeWorkoutStartedAtMillis)
            } else {
                preferences[Keys.activeWorkoutId] = session.workoutId
                val startedAt = session.startedAtMillis
                if (startedAt == null) preferences.remove(Keys.activeWorkoutStartedAtMillis)
                else preferences[Keys.activeWorkoutStartedAtMillis] = startedAt
            }
        }
    }

    override suspend fun logActiveWorkoutAndClear(completedAtMillis: Long, completedSets: Int): WorkoutLog? {
        var result: WorkoutLog? = null
        dataStore.edit { preferences ->
            val session = preferences.toActiveWorkoutSession()
            // Clearing belongs to the same transaction as the log, including missing legacy IDs.
            preferences.remove(Keys.activeWorkoutId)
            preferences.remove(Keys.activeWorkoutStartedAtMillis)
            if (session == null) return@edit
            val workout = decodeWorkouts(preferences[Keys.savedWorkouts].orEmpty())
                .firstOrNull { it.id == session.workoutId } ?: return@edit
            if (workout.totalSets == 0) return@edit

            val log = WorkoutResults.createLog(
                workout = workout,
                session = session,
                completedSets = completedSets,
                completedAtMillis = completedAtMillis,
                profile = preferences.toUserProfile(),
            )
            val currentLogs = decodeLogs(preferences[Keys.workoutLogs].orEmpty())
            preferences[Keys.workoutLogs] = (listOf(log) + currentLogs)
                .take(MaxWorkoutLogs)
                .joinToString(RecordSeparator, transform = ::encodeLog)
            preferences[Keys.pendingWorkoutSummary] = encodeLog(log)
            result = log
        }
        return result
    }

    override suspend fun dismissWorkoutSummary() {
        dataStore.edit { it.remove(Keys.pendingWorkoutSummary) }
    }

    private fun Preferences.toActiveWorkoutSession(): ActiveWorkoutSession? {
        val workoutId = this[Keys.activeWorkoutId]?.takeIf { it.isNotBlank() } ?: return null
        return ActiveWorkoutSession(workoutId, this[Keys.activeWorkoutStartedAtMillis])
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
            .distinctBy { it.id }
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

    private fun decodeWorkout(raw: String): PlannedWorkout? = runCatching { parseWorkout(raw) }.getOrNull()

    private fun parseWorkout(raw: String): PlannedWorkout? {
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
        val normalized = exercises.normalizedWorkoutExercises()
        if (normalized.isEmpty()) return null
        return PlannedWorkout(
            id = decode(parts[1]).takeIf { it.isNotBlank() } ?: return null,
            createdAtMillis = createdAt,
            name = decode(parts[3]).trim().ifBlank { "Workout" },
            exercises = normalized,
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
                runCatching { decodePlannedExercise(encodedExercise, fallbackRank = index + 1) }.getOrNull()
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
            log.durationMinutes?.toString().orEmpty(),
            log.calories.toString(),
            log.exerciseCount.toString(),
            log.startedAtMillis?.toString().orEmpty(),
            log.completedSets?.toString().orEmpty(),
            log.plannedSets?.toString().orEmpty(),
        ).joinToString(FieldSeparator)
    }

    private fun decodeLog(raw: String): WorkoutLog? = runCatching { parseLog(raw) }.getOrNull()

    private fun parseLog(raw: String): WorkoutLog? {
        val parts = raw.split(FieldSeparator)
        val legacy = parts.firstOrNull() == LegacyLogVersion
        if (legacy && parts.size != 6) return null
        if (!legacy && (parts.firstOrNull() != LogVersion || parts.size != 9)) return null
        return WorkoutLog(
            completedAtMillis = parts[1].toLongOrNull() ?: return null,
            name = decode(parts[2]).ifBlank { "Workout" },
            durationMinutes = if (parts[3].isEmpty() && !legacy) null else parts[3].toIntOrNull() ?: return null,
            calories = parts[4].toIntOrNull() ?: return null,
            exerciseCount = parts[5].toIntOrNull() ?: return null,
            startedAtMillis = parts.getOrNull(6)?.takeIf { it.isNotEmpty() }?.toLong(),
            completedSets = parts.getOrNull(7)?.takeIf { it.isNotEmpty() }?.toInt(),
            plannedSets = parts.getOrNull(8)?.takeIf { it.isNotEmpty() }?.toInt(),
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    // Uri used percent encoding, not form encoding: legacy literal plus signs must survive.
    private fun decode(value: String): String = URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")

    private object Keys {
        val ageYears = intPreferencesKey("age_years")
        val sex = stringPreferencesKey("sex")
        val weightKg = doublePreferencesKey("weight_kg")
        val heightCm = intPreferencesKey("height_cm")
        val savedWorkouts = stringPreferencesKey("saved_workouts")
        val workoutLogs = stringPreferencesKey("workout_logs")
        val activeWorkoutId = stringPreferencesKey("active_workout_id")
        val activeWorkoutStartedAtMillis = longPreferencesKey("active_workout_started_at_millis")
        val pendingWorkoutSummary = stringPreferencesKey("pending_workout_summary")
    }

    private companion object {
        val workoutLocks = WeakHashMap<DataStore<Preferences>, Mutex>()
        const val WorkoutVersion = "workout-v2"
        const val LegacyWorkoutVersion = "workout-v1"
        const val LogVersion = "log-v2"
        const val LegacyLogVersion = "log-v1"
        const val FieldSeparator = "|"
        const val ExerciseSeparator = ","
        const val ExerciseFieldSeparator = ":"
        const val RecordSeparator = "\n"
        const val MaxSavedWorkouts = 30
        const val MaxWorkoutLogs = 30
    }
}
