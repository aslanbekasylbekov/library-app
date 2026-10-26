package com.aslanbek.fitclub.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aslanbek.fitclub.domain.ClubRules
import com.aslanbek.fitclub.model.Trainer
import com.aslanbek.fitclub.model.TrainerRating
import com.aslanbek.fitclub.ui.components.CapacityBar
import com.aslanbek.fitclub.ui.components.EmptyState
import com.aslanbek.fitclub.ui.components.InfoRow
import com.aslanbek.fitclub.ui.components.ParticipationBadge
import com.aslanbek.fitclub.ui.components.SectionHeader
import com.aslanbek.fitclub.ui.components.WorkoutTypeIcon
import com.aslanbek.fitclub.ui.util.Formats
import com.aslanbek.fitclub.ui.util.ObserveAsEvents
import java.util.Locale

@Composable
fun WorkoutDetailsRoute(
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
    viewModel: WorkoutDetailsViewModel = viewModel(factory = WorkoutDetailsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.messages, onMessage)
    WorkoutDetailsScreen(state = state, onBack = onBack, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsScreen(
    state: WorkoutDetailsUiState,
    onBack: () -> Unit,
    onAction: (DetailsAction) -> Unit
) {
    var showLateCancelDialog by rememberSaveable { mutableStateOf(false) }
    val handleAction: (DetailsAction) -> Unit = { action ->
        if (action == DetailsAction.CANCEL && state.cancelIsLate) showLateCancelDialog = true else onAction(action)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.summary?.workout?.type?.label ?: "Class") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.notFound -> EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "Class not found",
                    message = "It may have been removed from the schedule."
                )
                else -> DetailsContent(state, handleAction)
            }
        }
    }

    if (showLateCancelDialog) {
        AlertDialog(
            onDismissRequest = { showLateCancelDialog = false },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            title = { Text("Late cancellation") },
            text = {
                Text(
                    "The class starts in less than ${ClubRules.FREE_CANCEL_HOURS} hours. Cancelling now counts as a strike. " +
                        "${ClubRules.STRIKE_LIMIT} strikes in ${ClubRules.STRIKE_WINDOW_DAYS} days pause booking."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLateCancelDialog = false
                    onAction(DetailsAction.CANCEL)
                }) { Text("Cancel anyway") }
            },
            dismissButton = {
                TextButton(onClick = { showLateCancelDialog = false }) { Text("Keep my spot") }
            }
        )
    }
}

@Composable
private fun DetailsContent(state: WorkoutDetailsUiState, onAction: (DetailsAction) -> Unit) {
    val summary = state.summary ?: return
    val workout = summary.workout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WorkoutTypeIcon(type = workout.type, size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = workout.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${workout.type.label} · ${workout.intensity.label} intensity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ParticipationBadge(summary)
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoRow(Icons.Filled.Schedule, Formats.whenText(workout, state.today))
                InfoRow(Icons.Filled.Timer, "${workout.durationMinutes} minutes")
                InfoRow(Icons.Filled.MeetingRoom, workout.room)
                InfoRow(Icons.Filled.LocalFireDepartment, "About ${workout.estimatedCalories} kcal")
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(text = state.hint, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (state.primaryAction != null) {
            ActionButtons(primary = state.primaryAction, secondary = state.secondaryAction, onAction = onAction)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Spots")
            CapacityBar(summary)
            if (state.attendees.isNotEmpty()) {
                InfoRow(Icons.Filled.Groups, attendeesText(state.attendees))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("About the class")
            Text(text = workout.description, style = MaterialTheme.typography.bodyLarge)
        }

        state.trainer?.let { trainer -> TrainerCard(trainer, state.rating) }
    }
}

@Composable
private fun TrainerCard(trainer: Trainer, rating: TrainerRating?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(trainer.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${trainer.specialty.label} coach",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RatingLabel(rating)
            }
            HorizontalDivider()
            Text(trainer.bio, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun RatingLabel(rating: TrainerRating?, modifier: Modifier = Modifier) {
    if (rating == null) {
        CircularProgressIndicator(modifier = modifier.size(20.dp), strokeWidth = 2.dp)
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = String.format(Locale.ENGLISH, "%.1f", rating.average),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = " (${rating.reviews})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButtons(
    primary: DetailsAction,
    secondary: DetailsAction?,
    onAction: (DetailsAction) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (secondary != null) {
            OutlinedButton(onClick = { onAction(secondary) }, modifier = Modifier.weight(1f)) {
                Text(secondary.label)
            }
        }
        val isDestructive = primary == DetailsAction.CANCEL || primary == DetailsAction.LEAVE_WAITLIST
        if (isDestructive) {
            OutlinedButton(
                onClick = { onAction(primary) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(primary.label) }
        } else {
            Button(onClick = { onAction(primary) }, modifier = Modifier.weight(1f)) {
                Text(primary.label)
            }
        }
    }
}

private fun attendeesText(names: List<String>): String {
    val shown = names.sortedByDescending { it == "You" }.take(3)
    val rest = names.size - shown.size
    return if (rest > 0) "${shown.joinToString(", ")} and $rest more" else shown.joinToString(", ")
}
