package com.restlock.domain

import kotlin.math.roundToInt

object FitnessCalculator {
    private const val ReferenceWeightKg = 70.0
    private const val MinutesPerDay = 1440.0

    fun caloriesForExercise(
        exercise: ExerciseDefinition,
        profile: UserProfile,
    ): Int {
        return caloriesForMet(
            met = exercise.met,
            minutes = exercise.defaultMinutes,
            profile = profile,
        )
    }

    fun caloriesForExercises(
        exerciseIds: List<String>,
        profile: UserProfile,
    ): Int {
        return exerciseIds.sumOf { id ->
            val exercise = ExerciseCatalog.byId(id) ?: return@sumOf 0
            caloriesForExercise(exercise, profile)
        }
    }

    fun caloriesForPlannedExercises(
        exercises: List<PlannedExercise>,
        profile: UserProfile,
    ): Int {
        return exercises.normalizedWorkoutExercises().sumOf { planned ->
            val exercise = ExerciseCatalog.byId(planned.exerciseId) ?: return@sumOf 0
            caloriesForExercise(exercise, profile) * planned.sets
        }
    }

    fun durationForExercises(exerciseIds: List<String>): Int {
        return exerciseIds.sumOf { id ->
            ExerciseCatalog.byId(id)?.defaultMinutes ?: 0
        }
    }

    fun durationForPlannedExercises(exercises: List<PlannedExercise>): Int {
        return exercises.normalizedWorkoutExercises().sumOf { planned ->
            (ExerciseCatalog.byId(planned.exerciseId)?.defaultMinutes ?: 0) * planned.sets
        }
    }

    fun restingMetabolicRate(profile: UserProfile): Int? {
        val age = profile.ageYears ?: return null
        val weight = profile.weightKg ?: return null
        val height = profile.heightCm ?: return null
        val sexAdjustment = when (profile.sex) {
            UserSex.Male -> 5.0
            UserSex.Female -> -161.0
            UserSex.Unspecified -> return null
        }
        return (9.99 * weight + 6.25 * height - 4.92 * age + sexAdjustment).roundToInt()
    }

    private fun caloriesForMet(
        met: Double,
        minutes: Int,
        profile: UserProfile,
    ): Int {
        val profileRmr = restingMetabolicRate(profile)
        val kcalPerMinute = if (profileRmr != null) {
            met * profileRmr / MinutesPerDay
        } else {
            met * 3.5 * (profile.weightKg ?: ReferenceWeightKg) / 200.0
        }
        return (kcalPerMinute * minutes).roundToInt().coerceAtLeast(0)
    }
}
