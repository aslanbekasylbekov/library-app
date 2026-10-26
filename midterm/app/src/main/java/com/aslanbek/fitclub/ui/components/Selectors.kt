package com.aslanbek.fitclub.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aslanbek.fitclub.model.WorkoutType
import com.aslanbek.fitclub.ui.util.Formats
import java.time.LocalDate

@Composable
fun <T> ChoiceChipRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> ImageVector?)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val optionIcon = if (isSelected) Icons.Filled.Check else icon?.invoke(option)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                leadingIcon = if (optionIcon != null) {
                    { Icon(optionIcon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
fun TypeFilterRow(
    selected: WorkoutType?,
    onSelect: (WorkoutType?) -> Unit,
    modifier: Modifier = Modifier
) {
    ChoiceChipRow(
        options = listOf<WorkoutType?>(null) + WorkoutType.entries,
        selected = selected,
        onSelect = onSelect,
        label = { it?.label ?: "All" },
        icon = { it?.icon },
        modifier = modifier
    )
}

data class DayOption(val date: LocalDate, val classes: Int)

@Composable
fun DaySelector(
    days: List<DayOption>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            val isSelected = day.date == selected
            Surface(
                selected = isSelected,
                onClick = { onSelect(day.date) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(64.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (day.date == today) "Today" else Formats.weekday(day.date),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(text = day.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge)
                    Text(text = "${day.classes} cl.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
