package com.restlock.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExerciseCatalogTest {
    @Test
    fun `catalog has unique ids and covers provided sections`() {
        val ids = ExerciseCatalog.exercises.map { it.id }

        assertThat(ids).containsNoDuplicates()
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.Biceps)).hasSize(21)
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.Triceps)).hasSize(18)
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.Back)).hasSize(23)
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.Legs)).hasSize(37)
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.Chest)).hasSize(19)
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.Shoulders)).hasSize(20)
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.Core)).hasSize(23)
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.Traps)).hasSize(8)
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.Forearms)).hasSize(10)
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.Neck)).hasSize(5)
        assertThat(ExerciseCatalog.byGroup(MuscleGroup.FullBody)).hasSize(17)
    }

    @Test
    fun `key legacy ids still resolve`() {
        assertThat(ExerciseCatalog.byId("dumbbell_curl")?.name).isEqualTo("Curl halteres debout")
        assertThat(ExerciseCatalog.byId("bench_press")?.name).isEqualTo("Bench press barre")
        assertThat(ExerciseCatalog.byId("overhead_press")?.muscleGroup).isEqualTo(MuscleGroup.Shoulders)
        assertThat(ExerciseCatalog.byId("rowing_machine")?.muscleGroup).isEqualTo(MuscleGroup.Cardio)
    }
}
