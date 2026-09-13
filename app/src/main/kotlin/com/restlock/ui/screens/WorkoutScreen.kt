package com.restlock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.restlock.domain.ExerciseCatalog
import com.restlock.domain.ExerciseDefinition
import com.restlock.domain.FitnessCalculator
import com.restlock.domain.MuscleGroup
import com.restlock.domain.PlannedWorkout
import com.restlock.ui.WorkoutUiState
import com.restlock.ui.WorkoutExerciseUiItem
import com.restlock.ui.WorkoutViewModel
import com.restlock.ui.components.ExerciseArtwork
import com.restlock.ui.components.GlassCard
import com.restlock.ui.components.PrimaryAction
import com.restlock.ui.components.SecondaryAction
import com.restlock.ui.theme.MintBrush
import com.restlock.ui.theme.PrimaryBrush
import com.restlock.ui.theme.RestLockPalette

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    sessionActive: Boolean,
    onStartSavedWorkout: (PlannedWorkout) -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    state.pendingDeletion?.let { workout ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteWorkout,
            title = { Text("Delete \"${workout.name}\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This removes the saved routine. Your workout history will not be deleted.")
                    state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteWorkout, enabled = !state.isDeleting) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteWorkout, enabled = !state.isDeleting) {
                    Text(
                        if (state.isDeleting) "Deleting..." else "Delete",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        WorkoutsTopBar()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.message?.let { message ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message, modifier = Modifier.weight(1f), color = RestLockPalette.TextMid)
                    IconButton(onClick = viewModel::dismissMessage) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss message")
                    }
                }
            }
            if (sessionActive) {
                Text("A workout is running. Finish or end it before editing routines or starting another workout.")
                SecondaryAction(label = "Return to session", onClick = onHome)
            }
            if (state.isBuilderOpen && !sessionActive) {
                WorkoutBuilder(
                    state = state,
                    onNameChange = viewModel::setWorkoutName,
                    onSelectGroup = viewModel::selectGroup,
                    onToggleExercise = viewModel::toggleExercise,
                    onSetExerciseSets = viewModel::setExerciseSets,
                    onSetExerciseReps = viewModel::setExerciseReps,
                    onMoveExerciseUp = viewModel::moveExerciseUp,
                    onMoveExerciseDown = viewModel::moveExerciseDown,
                    onSaveWorkout = viewModel::saveWorkout,
                    onClose = viewModel::closeBuilder,
                )
            } else {
                WorkoutOverview(
                    state = state,
                    onAddWorkout = viewModel::startAddingWorkout,
                    onEditWorkout = viewModel::startEditingWorkout,
                    onDeleteWorkout = viewModel::requestDeleteWorkout,
                    onStartSavedWorkout = onStartSavedWorkout,
                    sessionActive = sessionActive,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        MainBottomBar(
            selectedTab = MainTab.Workouts,
            onHome = onHome,
            onWorkouts = {},
            onSettings = onSettings,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun WorkoutsTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Workouts",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = RestLockPalette.TextHigh,
        )
        Text(
            text = "Create routines from the exercise catalog.",
            style = MaterialTheme.typography.bodyMedium,
            color = RestLockPalette.TextLow,
        )
    }
}

@Composable
private fun WorkoutOverview(
    state: WorkoutUiState,
    onAddWorkout: () -> Unit,
    onEditWorkout: (PlannedWorkout) -> Unit,
    onDeleteWorkout: (PlannedWorkout) -> Unit,
    onStartSavedWorkout: (PlannedWorkout) -> Unit,
    sessionActive: Boolean,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Add a workout",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = RestLockPalette.TextHigh,
                )
                Text(
                    text = "Pick a muscle section, add exercises, then save and start your workout.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextLow,
                )
            }
            PrimaryAction(
                label = "Add workout",
                onClick = onAddWorkout,
                enabled = !sessionActive,
                leadingIcon = Icons.Rounded.Add,
                brush = PrimaryBrush,
            )
        }
    }

    SavedWorkoutsCard(
        state = state,
        onEditWorkout = onEditWorkout,
        onDeleteWorkout = onDeleteWorkout,
        onStartSavedWorkout = onStartSavedWorkout,
        sessionActive = sessionActive,
    )

    ExerciseCatalogCard(state = state)
}

@Composable
private fun SavedWorkoutsCard(
    state: WorkoutUiState,
    onEditWorkout: (PlannedWorkout) -> Unit,
    onDeleteWorkout: (PlannedWorkout) -> Unit,
    onStartSavedWorkout: (PlannedWorkout) -> Unit,
    sessionActive: Boolean,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Saved workouts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
            )

            if (state.savedWorkouts.isEmpty()) {
                Text(
                    text = "No saved workouts yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RestLockPalette.TextMid,
                )
            } else {
                state.savedWorkouts.forEach { workout ->
                    SavedWorkoutRow(
                        workout = workout,
                        calories = FitnessCalculator.caloriesForPlannedExercises(
                            exercises = workout.exercises,
                            profile = state.profile,
                        ),
                        minutes = FitnessCalculator.durationForPlannedExercises(workout.exercises),
                        onEdit = { onEditWorkout(workout) },
                        onDelete = { onDeleteWorkout(workout) },
                        onStart = { onStartSavedWorkout(workout) },
                        sessionActive = sessionActive,
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedWorkoutRow(
    workout: PlannedWorkout,
    calories: Int,
    minutes: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
    sessionActive: Boolean,
) {
    var menuExpanded by remember(workout.id) { mutableStateOf(false) }
    val coverExercise = workout.orderedExercises
        .firstOrNull()
        ?.exerciseId
        ?.let(ExerciseCatalog::byId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (coverExercise != null) {
            ExerciseArtwork(exercise = coverExercise, modifier = Modifier.size(50.dp))
        } else {
            PlaceholderBadge()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${workout.exercises.size} exercises - ${workout.totalSets} sets - $minutes min - ~$calories kcal",
                style = MaterialTheme.typography.bodySmall,
                color = RestLockPalette.TextLow,
            )
        }
        IconButton(
            onClick = onStart,
            enabled = !sessionActive && workout.exercises.isNotEmpty() &&
                workout.exercises.all { ExerciseCatalog.byId(it.exerciseId) != null },
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Start ${workout.name}",
                tint = RestLockPalette.Mint,
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Actions for ${workout.name}", tint = RestLockPalette.TextMid)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    enabled = !sessionActive,
                    onClick = { menuExpanded = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun ExerciseCatalogCard(state: WorkoutUiState) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Exercise list",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
            )
            state.groups.forEach { group ->
                val count = ExerciseCatalog.byGroup(group).size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = group.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RestLockPalette.TextMid,
                    )
                    Text(
                        text = "$count exercises",
                        style = MaterialTheme.typography.labelMedium,
                        color = RestLockPalette.TextLow,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutBuilder(
    state: WorkoutUiState,
    onNameChange: (String) -> Unit,
    onSelectGroup: (MuscleGroup) -> Unit,
    onToggleExercise: (String) -> Unit,
    onSetExerciseSets: (String, Int) -> Unit,
    onSetExerciseReps: (String, Int) -> Unit,
    onMoveExerciseUp: (String) -> Unit,
    onMoveExerciseDown: (String) -> Unit,
    onSaveWorkout: () -> Unit,
    onClose: () -> Unit,
) {
    val selectedIds = state.selectedExercises.map { it.definition.id }.toSet()

    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 18) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isEditingWorkout) "Modify workout" else "Workout builder",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = RestLockPalette.TextHigh,
                    )
                    Text(
                        text = "${state.selectedExercises.size} exercises - ${state.selectedExercises.sumOf { it.plan.sets }} sets - ${state.estimatedMinutes} min - ~${state.estimatedCalories} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RestLockPalette.TextLow,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = RestLockPalette.TextMid,
                    )
                }
            }

            OutlinedTextField(
                value = state.workoutName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isSaving,
                label = { Text("Workout name") },
            )

            SelectedExercisePlan(
                selectedExercises = state.selectedExercises,
                onSetExerciseSets = onSetExerciseSets,
                onSetExerciseReps = onSetExerciseReps,
                onMoveExerciseUp = onMoveExerciseUp,
                onMoveExerciseDown = onMoveExerciseDown,
                onRemoveExercise = onToggleExercise,
            )

            MuscleGroupPicker(
                groups = state.groups,
                selectedGroup = state.selectedGroup,
                onSelectGroup = onSelectGroup,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.exercisesForSelectedGroup.forEach { exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        calories = FitnessCalculator.caloriesForExercise(exercise, state.profile),
                        selected = exercise.id in selectedIds,
                        onToggle = { onToggleExercise(exercise.id) },
                    )
                }
            }

            PrimaryAction(
                label = if (state.isSaving) "Saving..." else if (state.isEditingWorkout) "Save changes" else "Save workout",
                onClick = onSaveWorkout,
                leadingIcon = Icons.Rounded.Check,
                enabled = state.selectedExercises.isNotEmpty() && !state.isSaving,
                brush = PrimaryBrush,
            )
        }
    }
}

@Composable
private fun SelectedExercisePlan(
    selectedExercises: List<WorkoutExerciseUiItem>,
    onSetExerciseSets: (String, Int) -> Unit,
    onSetExerciseReps: (String, Int) -> Unit,
    onMoveExerciseUp: (String) -> Unit,
    onMoveExerciseDown: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
) {
    if (selectedExercises.isEmpty()) {
        Text(
            text = "No exercises selected yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = RestLockPalette.TextMid,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Workout order",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = RestLockPalette.TextHigh,
        )
        selectedExercises.forEachIndexed { index, item ->
            SelectedExerciseRow(
                item = item,
                canMoveUp = index > 0,
                canMoveDown = index < selectedExercises.lastIndex,
                onSetExerciseSets = onSetExerciseSets,
                onSetExerciseReps = onSetExerciseReps,
                onMoveExerciseUp = onMoveExerciseUp,
                onMoveExerciseDown = onMoveExerciseDown,
                onRemoveExercise = onRemoveExercise,
            )
        }
    }
}

@Composable
private fun SelectedExerciseRow(
    item: WorkoutExerciseUiItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSetExerciseSets: (String, Int) -> Unit,
    onSetExerciseReps: (String, Int) -> Unit,
    onMoveExerciseUp: (String) -> Unit,
    onMoveExerciseDown: (String) -> Unit,
    onRemoveExercise: (String) -> Unit,
) {
    val exerciseId = item.definition.id

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                ExerciseArtwork(
                    exercise = item.definition,
                    modifier = Modifier.size(46.dp),
                    cornerRadius = 16.dp,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(RestLockPalette.Ink0.copy(alpha = 0.82f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.plan.rank.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = RestLockPalette.Mint,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.definition.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = RestLockPalette.TextHigh,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.definition.section} - ${item.definition.equipment.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = RestLockPalette.TextLow,
                )
            }
            IconButton(
                onClick = { onMoveExerciseUp(exerciseId) },
                enabled = canMoveUp,
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Move exercise up",
                    tint = if (canMoveUp) RestLockPalette.TextMid else RestLockPalette.TextLow.copy(alpha = 0.35f),
                )
            }
            IconButton(
                onClick = { onMoveExerciseDown(exerciseId) },
                enabled = canMoveDown,
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Move exercise down",
                    tint = if (canMoveDown) RestLockPalette.TextMid else RestLockPalette.TextLow.copy(alpha = 0.35f),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CounterControl(
                label = "Sets",
                value = item.plan.sets,
                onDecrease = { onSetExerciseSets(exerciseId, item.plan.sets - 1) },
                onIncrease = { onSetExerciseSets(exerciseId, item.plan.sets + 1) },
                modifier = Modifier.weight(1f),
            )
            CounterControl(
                label = "Reps",
                value = item.plan.reps,
                onDecrease = { onSetExerciseReps(exerciseId, item.plan.reps - 1) },
                onIncrease = { onSetExerciseReps(exerciseId, item.plan.reps + 1) },
                modifier = Modifier.weight(1f),
            )
        }
        TextButton(onClick = { onRemoveExercise(exerciseId) }) {
            Text("Remove exercise", color = RestLockPalette.TextMid)
        }
    }
}

@Composable
private fun CounterControl(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onDecrease),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "Decrease $label",
                tint = RestLockPalette.TextMid,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.width(42.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = RestLockPalette.TextLow,
                maxLines = 1,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(RestLockPalette.Mint.copy(alpha = 0.14f))
                .clickable(onClick = onIncrease),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Increase $label",
                tint = RestLockPalette.Mint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun MuscleGroupPicker(
    groups: List<MuscleGroup>,
    selectedGroup: MuscleGroup,
    onSelectGroup: (MuscleGroup) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { group ->
            val selected = group == selectedGroup
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (selected) RestLockPalette.Mint else Color.White.copy(alpha = 0.07f)
                    )
                    .clickable { onSelectGroup(group) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) RestLockPalette.Ink0 else RestLockPalette.TextMid,
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseDefinition,
    calories: Int,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = if (selected) 0.11f else 0.05f))
            .clickable(onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ExerciseArtwork(exercise = exercise, modifier = Modifier.size(52.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = RestLockPalette.TextHigh,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${exercise.section} - ${exercise.equipment.label}",
                style = MaterialTheme.typography.bodySmall,
                color = RestLockPalette.TextLow,
            )
            Text(
                text = "${exercise.defaultMinutes} min - ${exercise.met} MET - ~$calories kcal",
                style = MaterialTheme.typography.labelSmall,
                color = RestLockPalette.TextLow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (selected) Icons.Rounded.Check else Icons.Rounded.Add,
                contentDescription = if (selected) "Remove exercise" else "Add exercise",
                tint = if (selected) RestLockPalette.Mint else RestLockPalette.TextMid,
            )
        }
    }
}

@Composable
private fun PlaceholderBadge() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MintBrush),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.FitnessCenter,
            contentDescription = null,
            tint = RestLockPalette.Ink0,
            modifier = Modifier.size(24.dp),
        )
    }
}
