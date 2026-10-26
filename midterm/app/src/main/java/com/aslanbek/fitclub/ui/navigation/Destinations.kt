package com.aslanbek.fitclub.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Insights
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(val route: String, val label: String, val icon: ImageVector) {
    SCHEDULE("schedule", "Schedule", Icons.Filled.CalendarMonth),
    BOOKINGS("bookings", "My classes", Icons.Filled.EventAvailable),
    PROGRESS("progress", "Progress", Icons.Filled.Insights)
}

object Routes {
    const val WORKOUT_ID = "workoutId"
    const val WORKOUT_DETAILS = "workout/{$WORKOUT_ID}"
    const val ADD_WORKOUT = "add_workout"

    fun workoutDetails(workoutId: Int): String = "workout/$workoutId"
}
