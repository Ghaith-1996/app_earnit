package com.restlock.domain

import kotlinx.coroutines.flow.Flow

interface FitnessRepository {
    val userProfile: Flow<UserProfile>
    val savedWorkouts: Flow<List<PlannedWorkout>>
    val workoutLogs: Flow<List<WorkoutLog>>
    val activeWorkoutId: Flow<String?>

    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun saveWorkout(workout: PlannedWorkout)
    suspend fun logWorkout(log: WorkoutLog)
    suspend fun setActiveWorkoutId(workoutId: String?)
    suspend fun logActiveWorkoutAndClear(completedAtMillis: Long): Boolean
}
