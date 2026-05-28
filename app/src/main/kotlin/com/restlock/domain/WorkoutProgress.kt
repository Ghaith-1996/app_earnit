package com.restlock.domain

data class ActiveExercisePreview(
    val plannedExercise: PlannedExercise,
    val definition: ExerciseDefinition,
    val exerciseRank: Int,
    val exerciseCount: Int,
    val setNumberForExercise: Int,
    val totalSetsForExercise: Int,
    val completedSetsInWorkout: Int,
    val totalSetsInWorkout: Int,
)

object WorkoutProgress {
    fun activeExercise(
        workout: PlannedWorkout?,
        completedSets: Int,
    ): ActiveExercisePreview? {
        val orderedExercises = workout?.orderedExercises.orEmpty()
        if (orderedExercises.isEmpty()) return null

        val completed = completedSets.coerceAtLeast(0)
        var previousSets = 0
        orderedExercises.forEachIndexed { index, planned ->
            val nextCompletedBoundary = previousSets + planned.sets
            if (completed < nextCompletedBoundary) {
                val definition = ExerciseCatalog.byId(planned.exerciseId) ?: return null
                return ActiveExercisePreview(
                    plannedExercise = planned,
                    definition = definition,
                    exerciseRank = index + 1,
                    exerciseCount = orderedExercises.size,
                    setNumberForExercise = completed - previousSets + 1,
                    totalSetsForExercise = planned.sets,
                    completedSetsInWorkout = completed,
                    totalSetsInWorkout = orderedExercises.sumOf { it.sets },
                )
            }
            previousSets = nextCompletedBoundary
        }

        return null
    }
}
