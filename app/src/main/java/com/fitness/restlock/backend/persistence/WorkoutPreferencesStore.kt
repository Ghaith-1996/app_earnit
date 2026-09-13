package com.fitness.restlock.backend.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutState
import com.fitness.restlock.backend.session.WorkoutSessionSnapshot
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.workoutDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "workout_backend",
)

class WorkoutPreferencesStore(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.workoutDataStore)

    val snapshots: Flow<WorkoutSessionSnapshot> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences -> preferences.toSnapshot() }

    suspend fun update(
        transform: (WorkoutSessionSnapshot) -> WorkoutSessionSnapshot,
    ): WorkoutSessionSnapshot {
        var nextSnapshot = WorkoutSessionSnapshot()
        dataStore.edit { preferences ->
            val current = preferences.toSnapshot()
            nextSnapshot = transform(current)
            preferences[Keys.mode] = nextSnapshot.mode.name
            preferences[Keys.restDurationSeconds] = nextSnapshot.restDurationSeconds
            preferences[Keys.allowedApps] = nextSnapshot.allowedApps
            preferences[Keys.timerEndEpochMillis] = nextSnapshot.timerEndEpochMillis
            preferences[Keys.completedSets] = nextSnapshot.completedSets
            preferences[Keys.extraRests] = nextSnapshot.extraRests
            val plannedSets = nextSnapshot.plannedSets
            if (plannedSets == null) preferences.remove(Keys.plannedSets)
            else preferences[Keys.plannedSets] = plannedSets
        }
        return nextSnapshot
    }

    private fun Preferences.toSnapshot(): WorkoutSessionSnapshot {
        val modeName = this[Keys.mode] ?: WorkoutMode.Idle.name
        val mode = runCatching { WorkoutMode.valueOf(modeName) }.getOrDefault(WorkoutMode.Idle)
        return WorkoutSessionSnapshot(
            mode = mode,
            restDurationSeconds = this[Keys.restDurationSeconds]
                ?: WorkoutState.DEFAULT_REST_DURATION_SECONDS,
            allowedApps = this[Keys.allowedApps].orEmpty(),
            timerEndEpochMillis = this[Keys.timerEndEpochMillis] ?: 0L,
            completedSets = this[Keys.completedSets] ?: 0,
            extraRests = this[Keys.extraRests] ?: 0,
            plannedSets = this[Keys.plannedSets]?.takeIf { it > 0 },
        )
    }

    private object Keys {
        val mode = stringPreferencesKey("mode")
        val restDurationSeconds = intPreferencesKey("rest_duration_seconds")
        val allowedApps = stringSetPreferencesKey("allowed_apps")
        val timerEndEpochMillis = longPreferencesKey("timer_end_epoch_millis")
        val completedSets = intPreferencesKey("completed_sets")
        val extraRests = intPreferencesKey("extra_rests")
        val plannedSets = intPreferencesKey("planned_sets")
    }
}
