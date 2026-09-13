package com.restlock.domain

/** Results derived from acknowledged sets; rest time never implies completed work. */
object WorkoutResults {
    fun completedExercises(workout: PlannedWorkout, completedSets: Int): List<PlannedExercise> {
        var remaining = completedSets.coerceIn(0, workout.totalSets)
        return workout.orderedExercises.mapNotNull { exercise ->
            val completed = remaining.coerceAtMost(exercise.sets)
            remaining -= completed
            // FitnessCalculator normalizes sets to at least one, so exclude untouched rows.
            exercise.copy(sets = completed).takeIf { completed > 0 }
        }
    }

    fun createLog(
        workout: PlannedWorkout,
        session: ActiveWorkoutSession,
        completedSets: Int,
        completedAtMillis: Long,
        profile: UserProfile,
    ): WorkoutLog {
        val completed = completedSets.coerceIn(0, workout.totalSets)
        val exercises = completedExercises(workout, completed)
        val durationMinutes = session.startedAtMillis?.let { start ->
            // Reject backwards wall-clock changes and saturate overflow instead of wrapping.
            val elapsed = if (completedAtMillis <= start) 0L else {
                runCatching { Math.subtractExact(completedAtMillis, start) }.getOrDefault(Long.MAX_VALUE)
            }
            (elapsed / 60_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
        return WorkoutLog(
            name = workout.name,
            completedAtMillis = completedAtMillis,
            durationMinutes = durationMinutes,
            calories = FitnessCalculator.caloriesForPlannedExercises(exercises, profile),
            exerciseCount = exercises.size,
            startedAtMillis = session.startedAtMillis,
            completedSets = completed,
            plannedSets = workout.totalSets,
        )
    }
}
