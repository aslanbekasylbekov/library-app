package com.aslanbek.fitclub.ui.bookings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aslanbek.fitclub.domain.WorkoutSummary
import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.ui.components.EmptyState
import com.aslanbek.fitclub.ui.components.StatusPill
import com.aslanbek.fitclub.ui.components.WorkoutCard
import com.aslanbek.fitclub.ui.components.WorkoutTypeIcon
import com.aslanbek.fitclub.ui.util.Formats
import com.aslanbek.fitclub.ui.util.ObserveAsEvents
import java.time.LocalDate

private enum class BookingsTab(val title: String) { UPCOMING("Upcoming"), WAITLIST("Waitlist"), HISTORY("History") }

@Composable
fun BookingsRoute(
    onOpenWorkout: (Int) -> Unit,
    onMessage: (String) -> Unit,
    viewModel: BookingsViewModel = viewModel(factory = BookingsViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.messages, onMessage)
    BookingsScreen(state = state, onOpenWorkout = onOpenWorkout, onCheckIn = viewModel::checkIn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    state: BookingsUiState,
    onOpenWorkout: (Int) -> Unit,
    onCheckIn: (Int) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tab = BookingsTab.entries[selectedTab]

    Scaffold(topBar = { TopAppBar(title = { Text("My classes") }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                BookingsTab.entries.forEachIndexed { index, item ->
                    val count = when (item) {
                        BookingsTab.UPCOMING -> state.upcoming.size
                        BookingsTab.WAITLIST -> state.waitlist.size
                        BookingsTab.HISTORY -> state.history.size
                    }
                    Tab(
                        selected = index == selectedTab,
                        onClick = { selectedTab = index },
                        text = { Text("${item.title} ($count)") }
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    BookingsTab.UPCOMING -> {
                        state.next?.let { next ->
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                NextClassCard(next, state.today, onOpenWorkout, onCheckIn)
                            }
                        }
                        summaryItems(
                            items = state.upcoming.drop(1),
                            today = state.today,
                            onOpenWorkout = onOpenWorkout,
                            emptyWhen = state.upcoming.isEmpty() && !state.isLoading,
                            empty = {
                                EmptyState(
                                    icon = Icons.Filled.EventAvailable,
                                    title = "No upcoming classes",
                                    message = "Open the schedule and book your next workout."
                                )
                            }
                        )
                    }
                    BookingsTab.WAITLIST -> summaryItems(
                        items = state.waitlist,
                        today = state.today,
                        onOpenWorkout = onOpenWorkout,
                        emptyWhen = state.waitlist.isEmpty() && !state.isLoading,
                        empty = {
                            EmptyState(
                                icon = Icons.Filled.HourglassEmpty,
                                title = "You are not on any waitlist",
                                message = "When a class is full, join its waitlist and we will book you when a spot opens."
                            )
                        }
                    )
                    BookingsTab.HISTORY -> {
                        if (state.history.isEmpty() && !state.isLoading) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyState(
                                    icon = Icons.Filled.History,
                                    title = "No history yet",
                                    message = "Classes you attended, missed or cancelled will appear here."
                                )
                            }
                        }
                        items(state.history, key = { it.bookingId }) { entry ->
                            WorkoutCard(
                                summary = entry.summary,
                                today = state.today,
                                onClick = { onOpenWorkout(entry.summary.id) },
                                showDate = true,
                                badge = { HistoryBadge(entry.status) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.summaryItems(
    items: List<WorkoutSummary>,
    today: LocalDate,
    onOpenWorkout: (Int) -> Unit,
    emptyWhen: Boolean,
    empty: @Composable () -> Unit
) {
    if (emptyWhen) {
        item(span = { GridItemSpan(maxLineSpan) }) { empty() }
    }
    items(items, key = { it.id }) { summary ->
        WorkoutCard(
            summary = summary,
            today = today,
            onClick = { onOpenWorkout(summary.id) },
            showDate = true
        )
    }
}

@Composable
private fun HistoryBadge(status: BookingStatus) {
    val colors = MaterialTheme.colorScheme
    when (status) {
        BookingStatus.ATTENDED -> StatusPill(status.label, colors.tertiaryContainer, colors.onTertiaryContainer)
        BookingStatus.NO_SHOW, BookingStatus.LATE_CANCELLED -> StatusPill(status.label, colors.errorContainer, colors.onErrorContainer)
        else -> StatusPill(status.label, colors.surfaceContainerHighest, colors.onSurfaceVariant)
    }
}

@Composable
private fun NextClassCard(
    next: NextClass,
    today: LocalDate,
    onOpenWorkout: (Int) -> Unit,
    onCheckIn: (Int) -> Unit
) {
    val workout = next.summary.workout
    Card(
        onClick = { onOpenWorkout(workout.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Next class", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                WorkoutTypeIcon(workout.type, size = 52.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(workout.title, style = MaterialTheme.typography.titleLarge)
                    Text(Formats.whenText(workout, today), style = MaterialTheme.typography.bodyMedium)
                    Text("${next.summary.trainerName} · ${workout.room}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(next.startsIn, style = MaterialTheme.typography.titleMedium)
            if (next.canCheckIn) {
                Button(
                    onClick = { onCheckIn(workout.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.HowToReg, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Check in now")
                }
            }
        }
    }
}
