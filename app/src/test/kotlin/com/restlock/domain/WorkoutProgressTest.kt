package com.restlock.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutProgressTest {
    @Test
    fun `every set follows uneven exercise boundaries without wrapping after completion`() {
        val workout = PlannedWorkout(
            id = "mixed", name = "Mixed", createdAtMillis = 1L,
            exercises = listOf(
                PlannedExercise("bench_press", sets = 3, rank = 2),
                PlannedExercise("leg_press", sets = 2, rank = 3),
                PlannedExercise("barbell_squat", sets = 1, rank = 1),
            ),
        )
        val expected = listOf(
            "barbell_squat" to 1, "bench_press" to 1, "bench_press" to 2,
            "bench_press" to 3, "leg_press" to 1, "leg_press" to 2,
        )
        expected.forEachIndexed { completed, (exerciseId, set) ->
            val preview = WorkoutProgress.activeExercise(workout, completed)!!
            assertThat(preview.definition.id).isEqualTo(exerciseId)
            assertThat(preview.setNumberForExercise).isEqualTo(set)
            assertThat(preview.totalSetsInWorkout).isEqualTo(6)
        }
        assertThat(WorkoutProgress.activeExercise(workout, -1)?.setNumberForExercise).isEqualTo(1)
        assertThat(WorkoutProgress.activeExercise(workout, 6)).isNull()
        assertThat(WorkoutProgress.activeExercise(workout, 99)).isNull()
        assertThat(WorkoutProgress.activeExercise(null, 0)).isNull()
        assertThat(WorkoutProgress.activeExercise(workout.copy(exercises = emptyList()), 0)).isNull()
    }

    @Test
    fun `active exercise follows rank order and set count`() {
        val workout = PlannedWorkout(
            id = "legs",
            name = "Leg day",
            exercises = listOf(
                PlannedExercise(exerciseId = "leg_press", sets = 2, reps = 12, rank = 2),
                PlannedExercise(exerciseId = "barbell_squat", sets = 3, reps = 8, rank = 1),
            ),
            createdAtMillis = 1L,
        )

        val first = WorkoutProgress.activeExercise(workout, completedSets = 0)
        val stillFirst = WorkoutProgress.activeExercise(workout, completedSets = 2)
        val second = WorkoutProgress.activeExercise(workout, completedSets = 3)
        val complete = WorkoutProgress.activeExercise(workout, completedSets = 5)

        assertThat(first?.definition?.id).isEqualTo("barbell_squat")
        assertThat(first?.setNumberForExercise).isEqualTo(1)
        assertThat(stillFirst?.definition?.id).isEqualTo("barbell_squat")
        assertThat(stillFirst?.setNumberForExercise).isEqualTo(3)
        assertThat(second?.definition?.id).isEqualTo("leg_press")
        assertThat(second?.setNumberForExercise).isEqualTo(1)
        assertThat(complete).isNull()
    }

    @Test
    fun `normalization clamps sets and reps and rewrites rank`() {
        val normalized = listOf(
            PlannedExercise(exerciseId = "bench_press", sets = 0, reps = 0, rank = 2),
            PlannedExercise(exerciseId = "push_up", sets = 99, reps = 999, rank = 1),
        ).normalizedWorkoutExercises()

        assertThat(normalized.map { it.exerciseId }).containsExactly("push_up", "bench_press").inOrder()
        assertThat(normalized.map { it.rank }).containsExactly(1, 2).inOrder()
        assertThat(normalized[0].sets).isEqualTo(PlannedExercise.MaxSets)
        assertThat(normalized[0].reps).isEqualTo(PlannedExercise.MaxReps)
        assertThat(normalized[1].sets).isEqualTo(PlannedExercise.MinSets)
        assertThat(normalized[1].reps).isEqualTo(PlannedExercise.MinReps)
    }
}
