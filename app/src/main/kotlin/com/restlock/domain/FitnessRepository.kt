package com.restlock.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class WorkoutMutationResult { Success, ActiveWorkout, InvalidWorkout, NotFound }

interface FitnessRepository {
    val userProfile: Flow<UserProfile>
    val savedWorkouts: Flow<List<PlannedWorkout>>
    val workoutLogs: Flow<List<WorkoutLog>>
    val activeWorkoutSession: Flow<ActiveWorkoutSession?>
    val activeWorkoutId: Flow<String?>
        get() = activeWorkoutSession.map { it?.workoutId }
    val pendingWorkoutSummary: Flow<WorkoutLog?>

    suspend fun saveUserProfile(profile: UserProfile)
    /** An edit requires its original routine to still exist; active routines are immutable. */
    suspend fun saveWorkout(workout: PlannedWorkout, requireExisting: Boolean = false): WorkoutMutationResult
    suspend fun deleteWorkout(workoutId: String): WorkoutMutationResult
    /** Serialize selecting and activating a routine with saved-routine mutations. */
    suspend fun <T> withWorkoutStartLock(action: suspend () -> T): T = action()
    suspend fun logWorkout(log: WorkoutLog)
    suspend fun setActiveWorkoutSession(session: ActiveWorkoutSession?)
    /** Atomically stores one result and its summary, then clears active metadata. */
    suspend fun logActiveWorkoutAndClear(completedAtMillis: Long, completedSets: Int): WorkoutLog?
    suspend fun dismissWorkoutSummary()
}
