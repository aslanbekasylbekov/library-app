package com.aslanbek.fitclub.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aslanbek.fitclub.domain.ClubRules
import com.aslanbek.fitclub.domain.ProgressStats
import com.aslanbek.fitclub.model.Membership
import com.aslanbek.fitclub.model.WorkoutType
import com.aslanbek.fitclub.ui.components.SectionHeader
import com.aslanbek.fitclub.ui.components.StatCard
import com.aslanbek.fitclub.ui.components.WorkoutTypeIcon
import com.aslanbek.fitclub.ui.components.color
import com.aslanbek.fitclub.ui.details.RatingLabel
import com.aslanbek.fitclub.ui.util.Formats
import java.util.Locale

@Composable
fun ProgressRoute(viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        state = state,
        onMembershipChange = viewModel::onMembershipChange,
        onGoalChange = viewModel::onGoalChange,
        onLiveActivityChange = viewModel::onLiveActivityChange,
        onRefreshRatings = { viewModel.loadRatings() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onMembershipChange: (Membership) -> Unit,
    onGoalChange: (Int) -> Unit,
    onLiveActivityChange: (Boolean) -> Unit,
    onRefreshRatings: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Progress") }) }) { innerPadding ->
        val stats = state.stats
        if (state.isLoading || stats == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            ProgressContent(state, stats, innerPadding, onMembershipChange, onGoalChange, onLiveActivityChange, onRefreshRatings)
        }
    }
}

@Composable
private fun ProgressContent(
    state: ProgressUiState,
    stats: ProgressStats,
    innerPadding: PaddingValues,
    onMembershipChange: (Membership) -> Unit,
    onGoalChange: (Int) -> Unit,
    onLiveActivityChange: (Boolean) -> Unit,
    onRefreshRatings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MembershipCard(state, onMembershipChange)
        WeeklyGoalCard(stats, onGoalChange)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Workouts", stats.attended.toString(), Modifier.weight(1f), Icons.Filled.FitnessCenter)
            StatCard("Minutes", stats.totalMinutes.toString(), Modifier.weight(1f), Icons.Filled.Timer)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("kcal burned", stats.totalCalories.toString(), Modifier.weight(1f), Icons.Filled.LocalFireDepartment)
            StatCard("Attendance", "${stats.attendanceRate}%", Modifier.weight(1f), Icons.Filled.CheckCircle)
        }

        SectionHeader("Last ${ProgressStats.WEEKS_IN_CHART} weeks")
        WeeksChart(stats)

        if (stats.minutesByType.isNotEmpty()) {
            SectionHeader("Minutes by workout type")
            MinutesByType(stats.minutesByType)
        }

        TrainersSection(state, onRefreshRatings)

        SectionHeader("Simulation")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Live club activity", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Other members book and cancel classes in the background, so spots change and the waitlist moves.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = state.liveActivity, onCheckedChange = onLiveActivityChange)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembershipCard(state: ProgressUiState, onMembershipChange: (Membership) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.memberName, style = MaterialTheme.typography.titleLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                Membership.entries.forEachIndexed { index, membership ->
                    SegmentedButton(
                        selected = membership == state.membership,
                        onClick = { onMembershipChange(membership) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Membership.entries.size)
                    ) {
                        Text(membership.label)
                    }
                }
            }
            val limit = state.membership.weeklyLimit
            Text(
                text = if (limit != null) {
                    "${state.membership.limitText}. Used this week: ${state.usedThisWeek} of $limit."
                } else {
                    "${state.membership.limitText}. Booked this week: ${state.usedThisWeek}."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            HorizontalDivider()
            val blockedUntil = state.blockedUntil
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (state.strikes > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (blockedUntil != null) {
                        "Booking paused until ${Formats.dayMonth(blockedUntil)}"
                    } else {
                        "Strikes: ${state.strikes} of ${ClubRules.STRIKE_LIMIT} in the last ${ClubRules.STRIKE_WINDOW_DAYS} days"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                "A no-show or a cancellation less than ${ClubRules.FREE_CANCEL_HOURS} h before the class is a strike.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyGoalCard(stats: ProgressStats, onGoalChange: (Int) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { stats.goalProgress },
                    modifier = Modifier.size(88.dp),
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
                Text("${stats.thisWeek}/${stats.weeklyGoal}", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Weekly goal", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Whatshot, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (stats.weekStreak == 1) "1-week streak" else "${stats.weekStreak}-week streak",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = { onGoalChange(stats.weeklyGoal - 1) },
                        enabled = stats.weeklyGoal > 1
                    ) { Icon(Icons.Filled.Remove, contentDescription = "Lower goal") }
                    Text(
                        text = "${stats.weeklyGoal} per week",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    FilledTonalIconButton(
                        onClick = { onGoalChange(stats.weeklyGoal + 1) },
                        enabled = stats.weeklyGoal < 7
                    ) { Icon(Icons.Filled.Add, contentDescription = "Raise goal") }
                }
            }
        }
    }
}

@Composable
private fun WeeksChart(stats: ProgressStats) {
    val max = (stats.lastWeeks.maxOfOrNull { it.workouts } ?: 0).coerceAtLeast(stats.weeklyGoal).coerceAtLeast(1)
    val maxBarHeight = 100.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        stats.lastWeeks.forEachIndexed { index, week ->
            val reached = week.workouts >= stats.weeklyGoal
            val isCurrent = index == stats.lastWeeks.lastIndex
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(week.workouts.toString(), style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxBarHeight * (week.workouts.toFloat() / max).coerceAtLeast(0.04f))
                        .background(
                            color = when {
                                reached -> MaterialTheme.colorScheme.primary
                                isCurrent -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isCurrent) "This" else Formats.dayMonth(week.weekStart),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MinutesByType(minutesByType: Map<WorkoutType, Int>) {
    val max = minutesByType.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        minutesByType.forEach { (type, minutes) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                WorkoutTypeIcon(type, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(type.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("$minutes min", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(minutes.toFloat() / max)
                                .height(8.dp)
                                .background(type.color, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainersSection(state: ProgressUiState, onRefreshRatings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("Trainers", Modifier.weight(1f))
            if (state.ratingsLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefreshRatings) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reload ratings")
                }
            }
        }
        state.ratingsTookMillis?.let { millis ->
            Text(
                text = String.format(
                    Locale.ENGLISH,
                    "%d ratings loaded in parallel in %.1f s",
                    state.trainers.size,
                    millis / 1000.0
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        state.trainers.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkoutTypeIcon(row.trainer.specialty, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.trainer.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${row.trainer.specialty.label} · ${row.classesTogether} classes with you",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RatingLabel(row.rating)
            }
        }
    }
}
