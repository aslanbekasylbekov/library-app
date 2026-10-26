package com.aslanbek.fitclub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aslanbek.fitclub.model.WorkoutType
import com.aslanbek.fitclub.ui.theme.CyclingColor
import com.aslanbek.fitclub.ui.theme.HiitColor
import com.aslanbek.fitclub.ui.theme.StrengthColor
import com.aslanbek.fitclub.ui.theme.SwimmingColor
import com.aslanbek.fitclub.ui.theme.YogaColor

val WorkoutType.icon: ImageVector
    get() = when (this) {
        WorkoutType.YOGA -> Icons.Filled.SelfImprovement
        WorkoutType.HIIT -> Icons.Filled.Bolt
        WorkoutType.CYCLING -> Icons.Filled.PedalBike
        WorkoutType.STRENGTH -> Icons.Filled.FitnessCenter
        WorkoutType.SWIMMING -> Icons.Filled.Pool
    }

val WorkoutType.color: Color
    get() = when (this) {
        WorkoutType.YOGA -> YogaColor
        WorkoutType.HIIT -> HiitColor
        WorkoutType.CYCLING -> CyclingColor
        WorkoutType.STRENGTH -> StrengthColor
        WorkoutType.SWIMMING -> SwimmingColor
    }

@Composable
fun WorkoutTypeIcon(
    type: WorkoutType,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(type.color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = type.label,
            tint = type.color,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
