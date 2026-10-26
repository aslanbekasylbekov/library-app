package com.aslanbek.fitclub.ui.addworkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aslanbek.fitclub.domain.NewWorkoutInput
import com.aslanbek.fitclub.domain.WorkoutField
import com.aslanbek.fitclub.domain.WorkoutValidation
import com.aslanbek.fitclub.model.Intensity
import com.aslanbek.fitclub.model.WorkoutType
import com.aslanbek.fitclub.ui.components.ChoiceChipRow
import com.aslanbek.fitclub.ui.components.SectionHeader
import com.aslanbek.fitclub.ui.components.icon
import com.aslanbek.fitclub.ui.util.Formats

@Composable
fun AddWorkoutRoute(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: AddWorkoutViewModel = viewModel(factory = AddWorkoutViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AddWorkoutScreen(
        state = state,
        onBack = onBack,
        onSubmit = { input ->
            when (val result = viewModel.save(input)) {
                is WorkoutValidation.Valid -> {
                    val workout = result.workout
                    onSaved("Added \"${workout.title}\" · ${Formats.whenText(workout, state.today)}")
                    null
                }
                is WorkoutValidation.Invalid -> result
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkoutScreen(
    state: AddWorkoutUiState,
    onBack: () -> Unit,
    onSubmit: (NewWorkoutInput) -> WorkoutValidation.Invalid?
) {
    var title by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(WorkoutType.YOGA) }
    var intensity by rememberSaveable { mutableStateOf(Intensity.MEDIUM) }
    var trainerId by rememberSaveable { mutableStateOf<Int?>(null) }
    var dayIndex by rememberSaveable { mutableIntStateOf(1) }
    var time by rememberSaveable { mutableStateOf("18:00") }
    var duration by rememberSaveable { mutableStateOf("60") }
    var capacity by rememberSaveable { mutableStateOf("12") }
    var room by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var errorField by rememberSaveable { mutableStateOf<WorkoutField?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    fun clearError(field: WorkoutField) {
        if (errorField == field) {
            errorField = null
            errorMessage = null
        }
    }

    val submit = {
        val input = NewWorkoutInput(
            title = title,
            type = type,
            intensity = intensity,
            trainerId = trainerId,
            date = state.days.getOrElse(dayIndex) { state.today },
            time = time,
            duration = duration,
            capacity = capacity,
            room = room,
            description = description
        )
        val error = onSubmit(input)
        errorField = error?.field
        errorMessage = error?.message
    }

    val sortedTrainers = state.trainers.sortedByDescending { it.specialty == type }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New class") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { TextButton(onClick = submit) { Text("Save") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormField(
                value = title,
                onValueChange = { title = it; clearError(WorkoutField.TITLE) },
                label = "Title",
                error = errorMessage.takeIf { errorField == WorkoutField.TITLE },
                capitalization = KeyboardCapitalization.Words
            )

            SectionHeader("Type")
            ChoiceChipRow(
                options = WorkoutType.entries,
                selected = type,
                onSelect = { selectedType ->
                    type = selectedType
                    if (trainerId == null) trainerId = state.trainers.firstOrNull { it.specialty == selectedType }?.id
                },
                label = { it.label },
                icon = { it.icon }
            )

            SectionHeader("Intensity")
            ChoiceChipRow(
                options = Intensity.entries,
                selected = intensity,
                onSelect = { intensity = it },
                label = { "${it.label} · ${it.kcalPerMinute} kcal/min" }
            )

            SectionHeader("Trainer")
            ChoiceChipRow(
                options = sortedTrainers.map { it.id },
                selected = trainerId ?: -1,
                onSelect = { trainerId = it; clearError(WorkoutField.TRAINER) },
                label = { id -> state.trainers.first { it.id == id }.name }
            )
            if (errorField == WorkoutField.TRAINER && errorMessage != null) {
                Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            SectionHeader("Day")
            ChoiceChipRow(
                options = state.days.indices.toList(),
                selected = dayIndex,
                onSelect = { dayIndex = it; clearError(WorkoutField.TIME) },
                label = { index -> Formats.relativeDay(state.days[index], state.today) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(
                    value = time,
                    onValueChange = { time = it; clearError(WorkoutField.TIME) },
                    label = "Start (HH:mm)",
                    error = errorMessage.takeIf { errorField == WorkoutField.TIME },
                    modifier = Modifier.weight(1f)
                )
                FormField(
                    value = duration,
                    onValueChange = { duration = it.filter(Char::isDigit); clearError(WorkoutField.DURATION) },
                    label = "Minutes",
                    error = errorMessage.takeIf { errorField == WorkoutField.DURATION },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField(
                    value = capacity,
                    onValueChange = { capacity = it.filter(Char::isDigit); clearError(WorkoutField.CAPACITY) },
                    label = "Capacity",
                    error = errorMessage.takeIf { errorField == WorkoutField.CAPACITY },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                FormField(
                    value = room,
                    onValueChange = { room = it; clearError(WorkoutField.ROOM) },
                    label = "Room",
                    error = errorMessage.takeIf { errorField == WorkoutField.ROOM },
                    capitalization = KeyboardCapitalization.Words,
                    modifier = Modifier.weight(1f)
                )
            }
            FormField(
                value = description,
                onValueChange = { description = it },
                label = "Description (optional)",
                singleLine = false,
                capitalization = KeyboardCapitalization.Sentences
            )
            Button(onClick = submit, modifier = Modifier.fillMaxWidth()) { Text("Add to schedule") }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
        )
    )
}
