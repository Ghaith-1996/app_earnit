package com.restlock.domain

enum class UserSex(val label: String) {
    Unspecified("Not set"),
    Male("Male"),
    Female("Female"),
}

data class UserProfile(
    val ageYears: Int? = null,
    val sex: UserSex = UserSex.Unspecified,
    val weightKg: Double? = null,
    val heightCm: Int? = null,
) {
    val isComplete: Boolean
        get() = ageYears != null &&
            sex != UserSex.Unspecified &&
            weightKg != null &&
            heightCm != null
}

enum class MuscleGroup(val label: String) {
    Biceps("Biceps"),
    Triceps("Triceps"),
    Back("Back"),
    Legs("Legs"),
    Chest("Chest"),
    Shoulders("Shoulders"),
    Core("Core"),
    Traps("Traps"),
    Forearms("Forearms"),
    Neck("Neck"),
    FullBody("Full body"),
    Cardio("Cardio"),
}

enum class ExerciseEquipment(val label: String) {
    Dumbbell("Dumbbell"),
    Barbell("Barbell"),
    Cable("Cable"),
    Machine("Machine"),
    Bodyweight("Bodyweight"),
    Kettlebell("Kettlebell"),
    Sled("Sled"),
    Conditioning("Conditioning"),
    Other("Other"),
}

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val section: String,
    val equipment: ExerciseEquipment,
    val met: Double,
    val defaultMinutes: Int,
    val metCategory: String,
)

data class PlannedExercise(
    val exerciseId: String,
    val sets: Int = DefaultSets,
    val reps: Int = DefaultReps,
    val rank: Int = 1,
) {
    companion object {
        const val DefaultSets = 3
        const val DefaultReps = 10
        const val MinSets = 1
        const val MaxSets = 20
        const val MinReps = 1
        const val MaxReps = 100
    }
}

data class PlannedWorkout(
    val id: String,
    val name: String,
    val exercises: List<PlannedExercise>,
    val createdAtMillis: Long,
) {
    val orderedExercises: List<PlannedExercise>
        get() = exercises.normalizedWorkoutExercises()

    val exerciseIds: List<String>
        get() = orderedExercises.map { it.exerciseId }

    val totalSets: Int
        get() = orderedExercises.sumOf { it.sets }

    val totalReps: Int
        get() = orderedExercises.sumOf { it.sets * it.reps }
}

/** A null start time preserves uncertainty for sessions created before timing was stored. */
data class ActiveWorkoutSession(
    val workoutId: String,
    val startedAtMillis: Long?,
)

data class WorkoutLog(
    val name: String,
    val completedAtMillis: Long,
    val durationMinutes: Int?,
    val calories: Int,
    val exerciseCount: Int,
    val startedAtMillis: Long? = null,
    val completedSets: Int? = null,
    val plannedSets: Int? = null,
) {
    /** Legacy records have no trustworthy set counts or completion status. */
    val completedFully: Boolean?
        get() = if (completedSets != null && plannedSets != null) {
            plannedSets > 0 && completedSets >= plannedSets
        } else {
            null
        }
}

/** Keep known, unique exercises in stable rank order, repairing bounds and contiguous ranks. */
fun List<PlannedExercise>.normalizedWorkoutExercises(): List<PlannedExercise> {
    return mapIndexed { index, exercise -> index to exercise }
        .sortedWith(compareBy<Pair<Int, PlannedExercise>> { it.second.rank }.thenBy { it.first })
        .filter { ExerciseCatalog.byId(it.second.exerciseId) != null }
        .distinctBy { it.second.exerciseId }
        .mapIndexed { index, exercise ->
            exercise.second.copy(
                sets = exercise.second.sets.coerceIn(PlannedExercise.MinSets, PlannedExercise.MaxSets),
                reps = exercise.second.reps.coerceIn(PlannedExercise.MinReps, PlannedExercise.MaxReps),
                rank = index + 1,
            )
        }
}
