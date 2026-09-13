package com.restlock.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutResultsTest {
    private val workout = PlannedWorkout(
        id = "push", name = "Push Day", createdAtMillis = 0L,
        exercises = listOf(
            PlannedExercise("shoulder_press_halteres", sets = 3, rank = 2),
            PlannedExercise("bench_press", sets = 4, rank = 1),
        ),
    )
    private val session = ActiveWorkoutSession("push", 1_000L)
    private val profile = UserProfile(weightKg = 80.0)

    @Test
    fun `completed work distributes in rank order without inventing zero-set work`() {
        val partial = WorkoutResults.completedExercises(workout, 5)
        assertThat(partial.map { it.exerciseId }).containsExactly("bench_press", "shoulder_press_halteres").inOrder()
        assertThat(partial.map { it.sets }).containsExactly(4, 1).inOrder()
        assertThat(WorkoutResults.completedExercises(workout, 3).map { it.sets }).containsExactly(3)
        assertThat(WorkoutResults.completedExercises(workout, 0)).isEmpty()
        assertThat(WorkoutResults.completedExercises(workout, -1)).isEmpty()
        assertThat(WorkoutResults.completedExercises(workout, Int.MAX_VALUE).sumOf { it.sets }).isEqualTo(7)
        for (sets in 0..7) {
            val exercises = WorkoutResults.completedExercises(workout, sets)
            assertThat(exercises.sumOf { it.sets }).isEqualTo(sets)
            assertThat(exercises.all { it.sets > 0 }).isTrue()
            assertThat(exercises.size).isEqualTo(if (sets == 0) 0 else if (sets <= 4) 1 else 2)
        }
    }

    @Test
    fun `full and partial results use actual elapsed time and completed work`() {
        val full = WorkoutResults.createLog(workout, session, 7, 61_000L, profile)
        val partial = WorkoutResults.createLog(workout, session, 3, 61_000L, profile)
        assertThat(full.name).isEqualTo("Push Day")
        assertThat(full.startedAtMillis).isEqualTo(1_000L)
        assertThat(full.completedAtMillis).isEqualTo(61_000L)
        assertThat(full.durationMinutes).isEqualTo(1)
        assertThat(full.completedSets).isEqualTo(7)
        assertThat(full.plannedSets).isEqualTo(7)
        assertThat(full.completedFully).isTrue()
        assertThat(full.exerciseCount).isEqualTo(2)
        assertThat(full.calories).isEqualTo(FitnessCalculator.caloriesForPlannedExercises(workout.exercises, profile))
        assertThat(partial.completedSets).isEqualTo(3)
        assertThat(partial.completedFully).isFalse()
        assertThat(partial.exerciseCount).isEqualTo(1)
        assertThat(partial.calories).isEqualTo(FitnessCalculator.caloriesForPlannedExercises(listOf(PlannedExercise("bench_press", sets = 3)), profile))
        assertThat(partial.calories).isAtMost(full.calories)
    }

    @Test
    fun `bounds and duration are safe and legacy start stays unknown`() {
        val zero = WorkoutResults.createLog(workout, session, -8, 0L, profile)
        assertThat(zero.completedSets).isEqualTo(0)
        assertThat(zero.exerciseCount).isEqualTo(0)
        assertThat(zero.calories).isEqualTo(0)
        assertThat(zero.durationMinutes).isEqualTo(0)
        assertThat(zero.completedFully).isFalse()
        assertThat(WorkoutResults.createLog(workout, session, 99, 120_999L, profile).durationMinutes).isEqualTo(1)
        assertThat(WorkoutResults.createLog(workout, session, 99, 61_000L, profile).completedSets).isEqualTo(7)
        val legacy = WorkoutResults.createLog(workout, session.copy(startedAtMillis = null), 3, 61_000L, profile)
        assertThat(legacy.durationMinutes).isNull()
        assertThat(legacy.startedAtMillis).isNull()
        assertThat(WorkoutLog("Old", 1L, 10, 100, 2).completedFully).isNull()
        assertThat(WorkoutResults.createLog(workout.copy(exercises = emptyList()), session, 0, 61_000L, profile).completedFully).isFalse()
        assertThat(WorkoutResults.createLog(workout, session.copy(startedAtMillis = Long.MIN_VALUE), 7, Long.MAX_VALUE, profile).durationMinutes).isEqualTo(Int.MAX_VALUE)
    }
}
