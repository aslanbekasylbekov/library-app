package com.aslanbek.fitclub.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aslanbek.fitclub.model.WorkoutType
import com.aslanbek.fitclub.ui.components.DaySelector
import com.aslanbek.fitclub.ui.components.EmptyState
import com.aslanbek.fitclub.ui.components.InfoRow
import com.aslanbek.fitclub.ui.components.LabeledCheckbox
import com.aslanbek.fitclub.ui.components.SearchField
import com.aslanbek.fitclub.ui.components.TypeFilterRow
import com.aslanbek.fitclub.ui.components.WorkoutCard
import com.aslanbek.fitclub.ui.util.Formats
import java.time.LocalDate

@Composable
fun ScheduleRoute(
    onOpenWorkout: (Int) -> Unit,
    onAddWorkout: () -> Unit,
    viewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScheduleScreen(
        state = state,
        onDaySelected = viewModel::onDaySelected,
        onQueryChange = viewModel::onQueryChange,
        onTypeSelected = viewModel::onTypeSelected,
        onOnlyAvailableChange = viewModel::onOnlyAvailableChange,
        onOpenWorkout = onOpenWorkout,
        onAddWorkout = onAddWorkout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    onDaySelected: (LocalDate) -> Unit,
    onQueryChange: (String) -> Unit,
    onTypeSelected: (WorkoutType?) -> Unit,
    onOnlyAvailableChange: (Boolean) -> Unit,
    onOpenWorkout: (Int) -> Unit,
    onAddWorkout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FitClub")
                        Text(
                            text = Formats.longDate(state.today),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddWorkout) {
                        Icon(Icons.Filled.Add, contentDescription = "Add a class")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.isBlocked) {
                        InfoRow(
                            icon = Icons.Filled.Block,
                            text = "Booking is paused: 3 strikes in the last 30 days",
                            tint = MaterialTheme.colorScheme.error
                        )
                    } else {
                        InfoRow(icon = Icons.Filled.WorkspacePremium, text = state.membershipText)
                    }
                    DaySelector(
                        days = state.days,
                        selected = state.selectedDay,
                        today = state.today,
                        onSelect = onDaySelected
                    )
                    SearchField(
                        query = state.query,
                        onQueryChange = onQueryChange,
                        placeholder = "Search class, trainer or room"
                    )
                    TypeFilterRow(selected = state.typeFilter, onSelect = onTypeSelected)
                    LabeledCheckbox(
                        text = "Only classes with free spots",
                        checked = state.onlyAvailable,
                        onCheckedChange = onOnlyAvailableChange
                    )
                }
            }

            if (!state.isLoading && state.workouts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = Icons.Filled.EventBusy,
                        title = "No classes found",
                        message = "Try another day or clear the filters."
                    )
                }
            }

            items(state.workouts, key = { it.id }) { summary ->
                WorkoutCard(
                    summary = summary,
                    today = state.today,
                    onClick = { onOpenWorkout(summary.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
