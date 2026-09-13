package com.restlock.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FitnessRepository {
    val userProfile: Flow<UserProfile>
    val savedWorkouts: Flow<List<PlannedWorkout>>
    val workoutLogs: Flow<List<WorkoutLog>>
    val activeWorkoutSession: Flow<ActiveWorkoutSession?>
    val activeWorkoutId: Flow<String?>
        get() = activeWorkoutSession.map { it?.workoutId }
    val pendingWorkoutSummary: Flow<WorkoutLog?>

    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun saveWorkout(workout: PlannedWorkout)
    suspend fun logWorkout(log: WorkoutLog)
    suspend fun setActiveWorkoutSession(session: ActiveWorkoutSession?)
    /** Atomically stores one result and its summary, then clears active metadata. */
    suspend fun logActiveWorkoutAndClear(completedAtMillis: Long, completedSets: Int): WorkoutLog?
    suspend fun dismissWorkoutSummary()
}
