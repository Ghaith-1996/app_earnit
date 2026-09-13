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
import com.restlock.domain.normalizedWorkoutExercises
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class WorkoutBuilderState(
    val isOpen: Boolean = false,
    val editingWorkoutId: String? = null,
    val editingCreatedAtMillis: Long? = null,
    val workoutName: String = "",
    val selectedGroup: MuscleGroup = MuscleGroup.Biceps,
    val plannedExercises: List<PlannedExercise> = emptyList(),
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
)

class WorkoutViewModel(
    private val fitnessRepository: FitnessRepository,
) : ViewModel() {
    private val builderState = MutableStateFlow(WorkoutBuilderState())

    val uiState: StateFlow<WorkoutUiState> = combine(
        builderState,
        fitnessRepository.savedWorkouts,
        fitnessRepository.userProfile,
    ) { builder, savedWorkouts, profile ->
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
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WorkoutUiState(),
    )

    fun startAddingWorkout() {
        builderState.value = WorkoutBuilderState(
            isOpen = true,
            workoutName = "New workout",
        )
    }

    fun startEditingWorkout(workout: PlannedWorkout) {
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
        builderState.value = WorkoutBuilderState()
    }

    fun setWorkoutName(name: String) {
        builderState.update { it.copy(workoutName = name) }
    }

    fun selectGroup(group: MuscleGroup) {
        builderState.update { it.copy(selectedGroup = group) }
    }

    fun toggleExercise(exerciseId: String) {
        builderState.update { current ->
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
        builderState.update { current ->
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
        builderState.update { current ->
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
        val plannedExercises = builder.plannedExercises.normalizedWorkoutExercises()
        if (plannedExercises.isEmpty()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            fitnessRepository.saveWorkout(
                PlannedWorkout(
                    id = builder.editingWorkoutId ?: "workout-$now",
                    name = builder.workoutName.ifBlank { "Workout" },
                    exercises = plannedExercises,
                    createdAtMillis = builder.editingCreatedAtMillis ?: now,
                )
            )
            closeBuilder()
        }
    }

}
