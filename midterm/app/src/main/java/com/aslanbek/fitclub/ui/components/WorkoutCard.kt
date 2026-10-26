package com.aslanbek.fitclub.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aslanbek.fitclub.domain.WorkoutSummary
import com.aslanbek.fitclub.model.Participation
import com.aslanbek.fitclub.ui.util.Formats
import java.time.LocalDate

@Composable
fun WorkoutCard(
    summary: WorkoutSummary,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = false,
    badge: @Composable () -> Unit = { ParticipationBadge(summary) }
) {
    val workout = summary.workout
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.widthIn(min = 56.dp)) {
                    if (showDate) {
                        Text(
                            text = Formats.relativeDay(workout.start.toLocalDate(), today),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(text = Formats.time(workout.start), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${workout.durationMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                WorkoutTypeIcon(type = workout.type)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = workout.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${summary.trainerName} · ${workout.room}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Whatshot,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${workout.intensity.label} · ~${workout.estimatedCalories} kcal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                badge()
            }
            CapacityBar(summary)
        }
    }
}

@Composable
fun ParticipationBadge(summary: WorkoutSummary, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    when (val participation = summary.participation) {
        Participation.Booked -> StatusPill("Booked", colors.tertiaryContainer, colors.onTertiaryContainer, modifier, Icons.Filled.CheckCircle)
        Participation.Attended -> StatusPill("Attended", colors.tertiaryContainer, colors.onTertiaryContainer, modifier, Icons.Filled.CheckCircle)
        Participation.Missed -> StatusPill("No-show", colors.errorContainer, colors.onErrorContainer, modifier)
        is Participation.Waitlisted -> StatusPill(
            "Waitlist #${participation.position}",
            colors.secondaryContainer,
            colors.onSecondaryContainer,
            modifier,
            Icons.Filled.HourglassTop
        )
        Participation.None -> when {
            summary.hasEnded -> StatusPill("Ended", colors.surfaceContainerHighest, colors.onSurfaceVariant, modifier)
            summary.hasStarted -> StatusPill("Started", colors.surfaceContainerHighest, colors.onSurfaceVariant, modifier)
            summary.isFull -> StatusPill("Full", colors.errorContainer, colors.onErrorContainer, modifier)
            else -> StatusPill("${summary.spotsLeft} left", colors.primaryContainer, colors.onPrimaryContainer, modifier)
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun CapacityBar(summary: WorkoutSummary, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val barColor = when {
        summary.isFull -> colors.error
        summary.fillRatio >= 0.75f -> colors.primary
        else -> colors.tertiary
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { summary.fillRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = barColor,
            trackColor = colors.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
        Text(
            text = buildString {
                append("${summary.takenSpots}/${summary.workout.capacity} booked")
                if (summary.waitlistSize > 0) append(" · ${summary.waitlistSize} on waitlist")
            },
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}
