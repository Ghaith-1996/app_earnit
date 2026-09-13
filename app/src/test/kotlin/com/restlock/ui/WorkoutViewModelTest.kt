package com.restlock.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.restlock.domain.ActiveWorkoutSession
import com.restlock.domain.FitnessCalculator
import com.restlock.domain.FitnessRepository
import com.restlock.domain.PlannedExercise
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.WorkoutLog
import com.restlock.domain.WorkoutMutationResult
import com.restlock.domain.backend.BackendFitnessRepository
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `empty or unknown only selection is rejected without a persistence call`() = runTest {
        val f = fixture()
        f.vm.startAddingWorkout()
        f.vm.toggleExercise("missing-from-catalog")
        f.vm.saveWorkout()
        runCurrent()
        assertThat(f.repository.saveCalls).isEqualTo(0)
        assertThat(f.repository.savedWorkouts.first()).isEmpty()
        assertThat(f.vm.uiState.value.isBuilderOpen).isTrue()
        assertThat(f.vm.uiState.value.message).contains("at least one exercise")
    }

    @Test
    fun `blank and padded names normalize and separate creations have unique identities`() = runTest {
        val f = fixture()
        for (name in listOf("   ", "  Push Day  ")) {
            f.vm.startAddingWorkout()
            f.vm.setWorkoutName(name)
            f.vm.toggleExercise("barbell_squat")
            f.vm.saveWorkout()
            runCurrent()
        }
        val saved = f.repository.savedWorkouts.first()
        assertThat(saved.map { it.name }).containsExactly("Workout", "Push Day")
        assertThat(saved.map { it.id }.toSet()).hasSize(2)
        assertThat(f.vm.uiState.value.isBuilderOpen).isFalse()
    }

    @Test
    fun `repeated toggles cannot save duplicate exercises`() = runTest {
        val f = fixture()
        f.vm.startAddingWorkout()
        repeat(9) { f.vm.toggleExercise("barbell_squat") }
        f.vm.toggleExercise("leg_press")
        f.vm.saveWorkout()
        runCurrent()
        val saved = f.repository.savedWorkouts.first().single()
        assertThat(saved.exerciseIds).containsExactly("barbell_squat", "leg_press").inOrder()
        assertThat(saved.exercises.map { it.rank }).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `sets and reps clamp at both domain bounds`() = runTest {
        val f = fixture()
        f.vm.startAddingWorkout()
        f.vm.toggleExercise("barbell_squat")
        f.vm.toggleExercise("leg_press")
        f.vm.setExerciseSets("barbell_squat", -5)
        f.vm.setExerciseReps("barbell_squat", 0)
        f.vm.setExerciseSets("leg_press", 999)
        f.vm.setExerciseReps("leg_press", 5_000)
        f.vm.saveWorkout()
        runCurrent()
        val exercises = f.repository.savedWorkouts.first().single().orderedExercises
        assertThat(exercises[0].sets).isEqualTo(PlannedExercise.MinSets)
        assertThat(exercises[0].reps).isEqualTo(PlannedExercise.MinReps)
        assertThat(exercises[1].sets).isEqualTo(PlannedExercise.MaxSets)
        assertThat(exercises[1].reps).isEqualTo(PlannedExercise.MaxReps)
    }

    @Test
    fun `moving exercises preserves ordering with contiguous ranks and safe boundaries`() = runTest {
        val f = fixture()
        f.vm.startAddingWorkout()
        listOf("barbell_squat", "leg_press", "dumbbell_curl").forEach(f.vm::toggleExercise)
        f.vm.moveExerciseUp("barbell_squat")
        f.vm.moveExerciseDown("dumbbell_curl")
        f.vm.moveExerciseUp("missing")
        runCurrent()
        assertThat(f.vm.uiState.value.selectedExercises.map { it.plan.exerciseId })
            .containsExactly("barbell_squat", "leg_press", "dumbbell_curl").inOrder()
        f.vm.moveExerciseUp("dumbbell_curl")
        f.vm.saveWorkout()
        runCurrent()
        val saved = f.repository.savedWorkouts.first().single()
        assertThat(saved.exerciseIds).containsExactly("barbell_squat", "dumbbell_curl", "leg_press").inOrder()
        assertThat(saved.exercises.map { it.rank }).containsExactly(1, 2, 3).inOrder()
    }

    @Test
    fun `edit preserves identity timestamp and unrelated routine`() = runTest {
        val f = fixture()
        val original = workout()
        val other = original.copy(id = "other", name = "Other", createdAtMillis = 42)
        f.repository.saveWorkout(original)
        f.repository.saveWorkout(other)
        f.vm.startEditingWorkout(original)
        runCurrent()
        f.vm.setWorkoutName("  Revised  ")
        f.vm.setExerciseSets("barbell_squat", 4)
        f.vm.saveWorkout()
        runCurrent()
        val saved = f.repository.savedWorkouts.first()
        assertThat(saved).hasSize(2)
        assertThat(saved.first { it.id == "other" }).isEqualTo(other)
        val edited = saved.first { it.id == original.id }
        assertThat(edited.createdAtMillis).isEqualTo(original.createdAtMillis)
        assertThat(edited.name).isEqualTo("Revised")
        assertThat(edited.orderedExercises.first().sets).isEqualTo(4)
    }

    @Test
    fun `removing an exercise updates ranks totals and both estimates`() = runTest {
        val f = fixture()
        val original = workout()
        f.repository.saveWorkout(original)
        f.vm.startEditingWorkout(original)
        runCurrent()
        val before = f.vm.uiState.value
        f.vm.toggleExercise("barbell_squat")
        runCurrent()
        val after = f.vm.uiState.value
        val plans = after.selectedExercises.map { it.plan }
        assertThat(plans.map { it.exerciseId }).containsExactly("leg_press")
        assertThat(plans.single().rank).isEqualTo(1)
        assertThat(plans.sumOf { it.sets }).isEqualTo(2)
        assertThat(plans.sumOf { it.sets * it.reps }).isEqualTo(24)
        assertThat(after.estimatedMinutes).isLessThan(before.estimatedMinutes)
        assertThat(after.estimatedCalories).isLessThan(before.estimatedCalories)
        assertThat(after.estimatedMinutes).isEqualTo(FitnessCalculator.durationForPlannedExercises(plans))
        assertThat(after.estimatedCalories).isEqualTo(FitnessCalculator.caloriesForPlannedExercises(plans, after.profile))
        f.vm.saveWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first().single().totalReps).isEqualTo(24)
    }

    @Test
    fun `removing the final exercise retains the saved version and rejects empty edit`() = runTest {
        val f = fixture()
        val original = workout().copy(exercises = listOf(PlannedExercise("barbell_squat")))
        f.repository.saveWorkout(original)
        f.vm.startEditingWorkout(original)
        runCurrent()
        f.vm.toggleExercise("barbell_squat")
        f.vm.saveWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).containsExactly(original)
        assertThat(f.vm.uiState.value.isBuilderOpen).isTrue()
    }

    @Test
    fun `active edit is blocked even without a UI subscription`() = runTest {
        val f = fixture(subscribe = false)
        val original = workout()
        f.repository.saveWorkout(original)
        f.repository.setActiveWorkoutSession(ActiveWorkoutSession(original.id, 100))
        f.vm.startEditingWorkout(original)
        runCurrent()
        subscribe(f.vm)
        runCurrent()
        assertThat(f.vm.uiState.value.isBuilderOpen).isFalse()
        assertThat(f.vm.uiState.value.message).contains("before editing")
        assertThat(f.repository.savedWorkouts.first()).containsExactly(original)
    }

    @Test
    fun `save rechecks active state after editing was opened`() = runTest {
        val f = fixture()
        val original = workout()
        f.repository.saveWorkout(original)
        f.vm.startEditingWorkout(original)
        runCurrent()
        f.vm.setWorkoutName("Changed during session")
        f.repository.setActiveWorkoutSession(ActiveWorkoutSession(original.id, 100))
        f.vm.saveWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).containsExactly(original)
        assertThat(f.vm.uiState.value.isBuilderOpen).isTrue()
        assertThat(f.vm.uiState.value.isSaving).isFalse()
        assertThat(f.vm.uiState.value.message).contains("before editing")
    }

    @Test
    fun `atomic repository guard rejects activation between VM check and save`() = runTest {
        val f = fixture()
        val original = workout()
        f.repository.saveWorkout(original)
        f.vm.startEditingWorkout(original)
        runCurrent()
        f.vm.setWorkoutName("Unsaved change")
        val gate = CompletableDeferred<Unit>()
        f.repository.saveGate = gate
        f.vm.saveWorkout()
        runCurrent()
        f.repository.setActiveWorkoutSession(ActiveWorkoutSession(original.id, 100))
        gate.complete(Unit)
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).containsExactly(original)
        assertThat(f.vm.uiState.value.message).contains("before editing")
        assertThat(f.vm.uiState.value.workoutName).isEqualTo("Unsaved change")
    }

    @Test
    fun `stale edit of a deleted workout never recreates it`() = runTest {
        val f = fixture()
        val original = workout()
        f.repository.saveWorkout(original)
        f.vm.startEditingWorkout(original)
        runCurrent()
        f.repository.deleteWorkout(original.id)
        f.vm.saveWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).isEmpty()
        assertThat(f.vm.uiState.value.isBuilderOpen).isTrue()
        assertThat(f.vm.uiState.value.message).contains("no longer exists")
    }

    @Test
    fun `double save writes once and late completion cannot close a newly opened builder`() = runTest {
        val f = fixture()
        val gate = CompletableDeferred<Unit>()
        f.repository.saveGate = gate
        f.vm.startAddingWorkout()
        f.vm.setWorkoutName("First")
        f.vm.toggleExercise("barbell_squat")
        f.vm.saveWorkout()
        f.vm.saveWorkout()
        runCurrent()
        assertThat(f.repository.saveCalls).isEqualTo(1)
        assertThat(f.vm.uiState.value.isSaving).isTrue()
        f.vm.closeBuilder()
        f.vm.startAddingWorkout()
        f.vm.setWorkoutName("Next")
        gate.complete(Unit)
        runCurrent()
        assertThat(f.repository.savedWorkouts.first().single().name).isEqualTo("First")
        assertThat(f.vm.uiState.value.isBuilderOpen).isTrue()
        assertThat(f.vm.uiState.value.workoutName).isEqualTo("Next")
        assertThat(f.vm.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `storage failure retains builder changes and allows retry`() = runTest {
        val f = fixture()
        f.repository.failSave = true
        f.vm.startAddingWorkout()
        f.vm.setWorkoutName("Keep my changes")
        f.vm.toggleExercise("barbell_squat")
        f.vm.saveWorkout()
        runCurrent()
        assertThat(f.vm.uiState.value.isBuilderOpen).isTrue()
        assertThat(f.vm.uiState.value.isSaving).isFalse()
        assertThat(f.vm.uiState.value.workoutName).isEqualTo("Keep my changes")
        assertThat(f.vm.uiState.value.selectedExercises).hasSize(1)
        assertThat(f.vm.uiState.value.message).contains("Couldn't save")
        assertThat(f.repository.savedWorkouts.first()).isEmpty()
        f.repository.failSave = false
        f.vm.saveWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first().single().name).isEqualTo("Keep my changes")
        assertThat(f.vm.uiState.value.isBuilderOpen).isFalse()
    }

    @Test
    fun `deletion requires confirmation cancel changes nothing and history survives confirmation`() = runTest {
        val f = fixture()
        val original = workout()
        val other = original.copy(id = "other", name = "Other")
        val log = WorkoutLog(original.name, 500, 5, 20, 1)
        f.repository.saveWorkout(original)
        f.repository.saveWorkout(other)
        f.repository.logWorkout(log)
        f.vm.requestDeleteWorkout(original)
        runCurrent()
        assertThat(f.vm.uiState.value.pendingDeletion).isEqualTo(original)
        assertThat(f.repository.savedWorkouts.first()).hasSize(2)
        assertThat(f.repository.deleteCalls).isEqualTo(0)
        f.vm.cancelDeleteWorkout()
        f.vm.confirmDeleteWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).hasSize(2)
        assertThat(f.repository.deleteCalls).isEqualTo(0)
        f.vm.requestDeleteWorkout(original)
        f.vm.confirmDeleteWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).containsExactly(other)
        assertThat(f.repository.workoutLogs.first()).containsExactly(log)
        assertThat(f.vm.uiState.value.pendingDeletion).isNull()
    }

    @Test
    fun `session starting after delete dialog opens blocks confirmation safely`() = runTest {
        val f = fixture()
        val original = workout()
        f.repository.saveWorkout(original)
        f.vm.requestDeleteWorkout(original)
        f.repository.setActiveWorkoutSession(ActiveWorkoutSession(original.id, 100))
        f.vm.confirmDeleteWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).containsExactly(original)
        assertThat(f.repository.activeWorkoutId.first()).isEqualTo(original.id)
        assertThat(f.vm.uiState.value.message).contains("before deleting")
    }

    @Test
    fun `confirming an already removed workout leaves other routines intact`() = runTest {
        val f = fixture()
        val original = workout()
        val other = original.copy(id = "other", name = "Other")
        f.repository.saveWorkout(original)
        f.repository.saveWorkout(other)
        f.vm.requestDeleteWorkout(original)
        f.repository.deleteWorkout(original.id)
        f.vm.confirmDeleteWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).containsExactly(other)
        assertThat(f.vm.uiState.value.message).contains("no longer exists")
        assertThat(f.vm.uiState.value.pendingDeletion).isNull()
    }

    @Test
    fun `delete storage failure keeps confirmation available for retry`() = runTest {
        val f = fixture()
        val original = workout()
        f.repository.saveWorkout(original)
        f.repository.failDelete = true
        f.vm.requestDeleteWorkout(original)
        f.vm.confirmDeleteWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).containsExactly(original)
        assertThat(f.vm.uiState.value.pendingDeletion).isEqualTo(original)
        assertThat(f.vm.uiState.value.isDeleting).isFalse()
        assertThat(f.vm.uiState.value.message).contains("Couldn't delete")
        f.repository.failDelete = false
        f.vm.confirmDeleteWorkout()
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).isEmpty()
        assertThat(f.vm.uiState.value.pendingDeletion).isNull()
    }

    @Test
    fun `deleting a routine cancels an edit request still loading that routine`() = runTest {
        val f = fixture()
        val original = workout()
        f.repository.saveWorkout(original)
        val gate = CompletableDeferred<Unit>()
        f.repository.savedReadGate = gate
        f.vm.startEditingWorkout(original)
        runCurrent()
        f.vm.requestDeleteWorkout(original)
        f.vm.confirmDeleteWorkout()
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertThat(f.repository.savedWorkouts.first()).isEmpty()
        assertThat(f.vm.uiState.value.isBuilderOpen).isFalse()
    }

    private fun TestScope.fixture(subscribe: Boolean = true): Fixture {
        val file = File(temporaryFolder.newFolder(), "fitness.preferences_pb")
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        val repository = ControlledRepository(BackendFitnessRepository(store))
        val vm = WorkoutViewModel(repository)
        if (subscribe) subscribe(vm)
        runCurrent()
        return Fixture(repository, vm)
    }

    private fun TestScope.subscribe(vm: WorkoutViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
    }

    private fun workout() = PlannedWorkout(
        id = "legs", name = "Leg day", createdAtMillis = 123,
        exercises = listOf(
            PlannedExercise("barbell_squat", sets = 4, reps = 8, rank = 1),
            PlannedExercise("leg_press", sets = 2, reps = 12, rank = 2),
        ),
    )

    private data class Fixture(val repository: ControlledRepository, val vm: WorkoutViewModel)

    /** Delays/failures wrap the actual file-backed repository rather than replacing its rules. */
    private class ControlledRepository(private val delegate: FitnessRepository) : FitnessRepository by delegate {
        var saveCalls = 0
        var deleteCalls = 0
        var saveGate: CompletableDeferred<Unit>? = null
        var savedReadGate: CompletableDeferred<Unit>? = null
        var failSave = false
        var failDelete = false

        override val savedWorkouts = flow {
            delegate.savedWorkouts.collect { workouts ->
                savedReadGate?.await()
                emit(workouts)
            }
        }

        override suspend fun saveWorkout(workout: PlannedWorkout, requireExisting: Boolean): WorkoutMutationResult {
            saveCalls++
            saveGate?.await()
            if (failSave) throw IOException("Storage unavailable")
            return delegate.saveWorkout(workout, requireExisting)
        }

        override suspend fun deleteWorkout(workoutId: String): WorkoutMutationResult {
            deleteCalls++
            if (failDelete) throw IOException("Storage unavailable")
            return delegate.deleteWorkout(workoutId)
        }
    }
}
