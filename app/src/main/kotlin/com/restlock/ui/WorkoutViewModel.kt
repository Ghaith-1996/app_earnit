package com.restlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.restlock.domain.ExerciseCatalog
import com.restlock.domain.ExerciseDefinition
import com.restlock.domain.FitnessCalculator
import com.restlock.domain.FitnessRepository
import com.restlock.domain.MuscleGroup
import com.restlock.domain.PlannedExercise
import com.restlock.domain.PlannedWorkout
import com.restlock.domain.UserProfile
import com.restlock.domain.WorkoutMutationResult
import com.restlock.domain.normalizedWorkoutExercises
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class WorkoutBuilderState(
    val token: String = UUID.randomUUID().toString(),
    val isOpen: Boolean = false,
    val isSaving: Boolean = false,
    val editingWorkoutId: String? = null,
    val editingCreatedAtMillis: Long? = null,
    val workoutName: String = "",
    val selectedGroup: MuscleGroup = MuscleGroup.Biceps,
    val plannedExercises: List<PlannedExercise> = emptyList(),
)

private data class WorkoutManagementState(
    val message: String? = null,
    val pendingDeletion: PlannedWorkout? = null,
    val isDeleting: Boolean = false,
)

data class WorkoutExerciseUiItem(
    val plan: PlannedExercise,
    val definition: ExerciseDefinition,
)

data class WorkoutUiState(
    val isBuilderOpen: Boolean = false,
    val isEditingWorkout: Boolean = false,
    val workoutName: String = "",
    val selectedGroup: MuscleGroup = MuscleGroup.Biceps,
    val groups: List<MuscleGroup> = ExerciseCatalog.groups,
    val exercisesForSelectedGroup: List<ExerciseDefinition> = ExerciseCatalog.byGroup(MuscleGroup.Biceps),
    val selectedExercises: List<WorkoutExerciseUiItem> = emptyList(),
    val savedWorkouts: List<PlannedWorkout> = emptyList(),
    val estimatedCalories: Int = 0,
    val estimatedMinutes: Int = 0,
    val profile: UserProfile = UserProfile(),
    val isSaving: Boolean = false,
    val message: String? = null,
    val pendingDeletion: PlannedWorkout? = null,
    val isDeleting: Boolean = false,
)

class WorkoutViewModel(
    private val fitnessRepository: FitnessRepository,
) : ViewModel() {
    private val builderState = MutableStateFlow(WorkoutBuilderState())
    private val managementState = MutableStateFlow(WorkoutManagementState())
    private var editRequest = 0L

    val uiState: StateFlow<WorkoutUiState> = combine(
        builderState,
        fitnessRepository.savedWorkouts,
        fitnessRepository.userProfile,
        managementState,
    ) { builder, savedWorkouts, profile, management ->
        val selectedExercises = builder.plannedExercises
            .normalizedWorkoutExercises()
            .mapNotNull { planned ->
                ExerciseCatalog.byId(planned.exerciseId)?.let { definition ->
                    WorkoutExerciseUiItem(plan = planned, definition = definition)
                }
            }
        WorkoutUiState(
            isBuilderOpen = builder.isOpen,
            isEditingWorkout = builder.editingWorkoutId != null,
            workoutName = builder.workoutName,
            selectedGroup = builder.selectedGroup,
            exercisesForSelectedGroup = ExerciseCatalog.byGroup(builder.selectedGroup),
            selectedExercises = selectedExercises,
            savedWorkouts = savedWorkouts,
            estimatedCalories = FitnessCalculator.caloriesForPlannedExercises(
                exercises = builder.plannedExercises,
                profile = profile,
            ),
            estimatedMinutes = FitnessCalculator.durationForPlannedExercises(builder.plannedExercises),
            profile = profile,
            isSaving = builder.isSaving,
            message = management.message,
            pendingDeletion = management.pendingDeletion,
            isDeleting = management.isDeleting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutUiState(),
    )

    fun startAddingWorkout() {
        editRequest++
        dismissMessage()
        builderState.value = WorkoutBuilderState(
            isOpen = true,
            workoutName = "New workout",
        )
    }

    fun startEditingWorkout(workout: PlannedWorkout) {
        val request = ++editRequest
        dismissMessage()
        viewModelScope.launch {
            try {
                val saved = fitnessRepository.savedWorkouts.first().firstOrNull { it.id == workout.id }
                val activeId = fitnessRepository.activeWorkoutId.first()
                if (request != editRequest) return@launch
                if (activeId == workout.id) {
                    showMessage(ActiveEditMessage)
                    return@launch
                }
                if (saved == null) {
                    showMessage("This workout no longer exists.")
                    return@launch
                }
                openEditor(saved)
            } catch (_: IOException) {
                if (request == editRequest) showMessage("Couldn't open the workout. Please try again.")
            }
        }
    }

    private fun openEditor(workout: PlannedWorkout) {
        builderState.value = WorkoutBuilderState(
            isOpen = true,
            editingWorkoutId = workout.id,
            editingCreatedAtMillis = workout.createdAtMillis,
            workoutName = workout.name,
            selectedGroup = workout.orderedExercises
                .firstOrNull()
                ?.exerciseId
                ?.let(ExerciseCatalog::byId)
                ?.muscleGroup
                ?: MuscleGroup.Biceps,
            plannedExercises = workout.orderedExercises,
        )
    }

    fun closeBuilder() {
        editRequest++
        builderState.value = WorkoutBuilderState()
    }

    fun setWorkoutName(name: String) {
        updateBuilder { it.copy(workoutName = name) }
    }

    fun selectGroup(group: MuscleGroup) {
        updateBuilder { it.copy(selectedGroup = group) }
    }

    fun toggleExercise(exerciseId: String) {
        if (ExerciseCatalog.byId(exerciseId) == null) return
        updateBuilder { current ->
            val selected = current.plannedExercises.normalizedWorkoutExercises()
            val next = (if (selected.any { it.exerciseId == exerciseId }) {
                selected.filterNot { it.exerciseId == exerciseId }
            } else {
                selected + PlannedExercise(
                    exerciseId = exerciseId,
                    rank = selected.size + 1,
                )
            }).normalizedWorkoutExercises()
            current.copy(plannedExercises = next)
        }
    }

    fun setExerciseSets(exerciseId: String, sets: Int) {
        updatePlannedExercise(exerciseId) { exercise ->
            exercise.copy(sets = sets.coerceIn(PlannedExercise.MinSets, PlannedExercise.MaxSets))
        }
    }

    fun setExerciseReps(exerciseId: String, reps: Int) {
        updatePlannedExercise(exerciseId) { exercise ->
            exercise.copy(reps = reps.coerceIn(PlannedExercise.MinReps, PlannedExercise.MaxReps))
        }
    }

    fun moveExerciseUp(exerciseId: String) {
        moveExercise(exerciseId = exerciseId, offset = -1)
    }

    fun moveExerciseDown(exerciseId: String) {
        moveExercise(exerciseId = exerciseId, offset = 1)
    }

    private fun updatePlannedExercise(
        exerciseId: String,
        transform: (PlannedExercise) -> PlannedExercise,
    ) {
        updateBuilder { current ->
            current.copy(
                plannedExercises = current.plannedExercises
                    .normalizedWorkoutExercises()
                    .map { exercise ->
                        if (exercise.exerciseId == exerciseId) transform(exercise) else exercise
                    }
                    .normalizedWorkoutExercises(),
            )
        }
    }

    private fun moveExercise(exerciseId: String, offset: Int) {
        updateBuilder { current ->
            val exercises = current.plannedExercises.normalizedWorkoutExercises().toMutableList()
            val from = exercises.indexOfFirst { it.exerciseId == exerciseId }
            val to = from + offset
            if (from !in exercises.indices || to !in exercises.indices) {
                current
            } else {
                val moved = exercises.removeAt(from)
                exercises.add(to, moved)
                current.copy(plannedExercises = exercises.mapIndexed { index, exercise ->
                    exercise.copy(rank = index + 1)
                })
            }
        }
    }

    fun saveWorkout() {
        val builder = builderState.value
        if (!builder.isOpen || builder.isSaving) return
        val plannedExercises = builder.plannedExercises.normalizedWorkoutExercises()
        if (plannedExercises.isEmpty()) {
            showMessage("Add at least one exercise before saving.")
            return
        }
        builderState.value = builder.copy(isSaving = true)
        dismissMessage()

        viewModelScope.launch {
            try {
                if (builder.editingWorkoutId != null &&
                    fitnessRepository.activeWorkoutId.first() == builder.editingWorkoutId
                ) {
                    if (builderState.value.token == builder.token) showMessage(ActiveEditMessage)
                    return@launch
                }
                val now = System.currentTimeMillis()
                val result = fitnessRepository.saveWorkout(
                    PlannedWorkout(
                        id = builder.editingWorkoutId ?: "workout-${builder.token}",
                        name = builder.workoutName.trim().ifBlank { "Workout" },
                        exercises = plannedExercises,
                        createdAtMillis = builder.editingCreatedAtMillis ?: now,
                    ),
                    requireExisting = builder.editingWorkoutId != null,
                )
                // A save finishing after Close / Add / Edit must not close the newer builder.
                if (builderState.value.token != builder.token) return@launch
                when (result) {
                    WorkoutMutationResult.Success -> closeBuilder()
                    WorkoutMutationResult.ActiveWorkout -> showMessage(ActiveEditMessage)
                    WorkoutMutationResult.InvalidWorkout -> showMessage("Add at least one valid exercise before saving.")
                    WorkoutMutationResult.NotFound -> showMessage("This workout no longer exists. Close the builder to create a new routine.")
                }
            } catch (_: IOException) {
                if (builderState.value.token == builder.token) {
                    showMessage("Couldn't save the workout. Your changes are still here. Please try again.")
                }
            } finally {
                builderState.update { current ->
                    if (current.token == builder.token) current.copy(isSaving = false) else current
                }
            }
        }
    }

    fun requestDeleteWorkout(workout: PlannedWorkout) {
        if (managementState.value.isDeleting) return
        managementState.value = WorkoutManagementState(pendingDeletion = workout)
    }

    fun cancelDeleteWorkout() {
        if (!managementState.value.isDeleting) {
            managementState.update { it.copy(pendingDeletion = null) }
        }
    }

    fun confirmDeleteWorkout() {
        val management = managementState.value
        val workout = management.pendingDeletion ?: return
        if (management.isDeleting) return
        managementState.value = management.copy(isDeleting = true, message = null)
        viewModelScope.launch {
            try {
                // The repository checks active identity inside its atomic edit, including
                // sessions started after the confirmation dialog was opened.
                when (fitnessRepository.deleteWorkout(workout.id)) {
                    WorkoutMutationResult.Success -> {
                        editRequest++ // Invalidate an editor still loading the deleted routine.
                        if (builderState.value.editingWorkoutId == workout.id) closeBuilder()
                        showMessage("Workout deleted. Your workout history is unchanged.")
                    }
                    WorkoutMutationResult.ActiveWorkout -> showMessage(
                        "This workout is currently active. Finish or end the workout before deleting it."
                    )
                    WorkoutMutationResult.NotFound -> showMessage("This workout no longer exists.")
                    WorkoutMutationResult.InvalidWorkout -> showMessage("Couldn't delete this workout.")
                }
                managementState.update { it.copy(pendingDeletion = null) }
            } catch (_: IOException) {
                showMessage("Couldn't delete the workout. Please try again.")
            } finally {
                managementState.update { it.copy(isDeleting = false) }
            }
        }
    }

    fun dismissMessage() {
        managementState.update { it.copy(message = null) }
    }

    private fun showMessage(message: String) {
        managementState.update { it.copy(message = message) }
    }

    private fun updateBuilder(transform: (WorkoutBuilderState) -> WorkoutBuilderState) {
        builderState.update { if (it.isOpen && !it.isSaving) transform(it) else it }
    }

    private companion object {
        const val ActiveEditMessage = "This workout is currently active. Finish or end the workout before editing it."
    }
}
