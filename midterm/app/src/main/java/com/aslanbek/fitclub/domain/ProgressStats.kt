package com.aslanbek.fitclub.domain

import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.model.Workout
import com.aslanbek.fitclub.model.WorkoutType
import java.time.LocalDate

data class WeekBar(val weekStart: LocalDate, val workouts: Int)

data class ProgressStats(
    val attended: Int,
    val missed: Int,
    val totalMinutes: Int,
    val totalCalories: Int,
    val minutesByType: Map<WorkoutType, Int>,
    val favoriteType: WorkoutType?,
    val thisWeek: Int,
    val weeklyGoal: Int,
    val weekStreak: Int,
    val lastWeeks: List<WeekBar>
) {
    val attendanceRate: Int
        get() = if (attended + missed == 0) 100 else attended * 100 / (attended + missed)

    val goalProgress: Float
        get() = (thisWeek.toFloat() / weeklyGoal).coerceIn(0f, 1f)

    companion object {
        const val WEEKS_IN_CHART = 6

        fun from(state: ClubState, memberId: Int = state.currentMemberId): ProgressStats {
            val history = state.bookingsOf(memberId)
            val attendedWorkouts: List<Workout> = history
                .filter { it.status == BookingStatus.ATTENDED }
                .mapNotNull { state.workout(it.workoutId) }

            val minutesByType = attendedWorkouts
                .groupBy { it.type }
                .mapValues { (_, workouts) -> workouts.sumOf { it.durationMinutes } }
                .toList()
                .sortedByDescending { it.second }
                .toMap()

            val countsByWeek: Map<LocalDate, Int> = attendedWorkouts
                .groupingBy { it.start.toLocalDate().startOfWeek() }
                .eachCount()

            val currentWeek = state.today.startOfWeek()
            val lastWeeks = (WEEKS_IN_CHART - 1 downTo 0).map { weeksAgo ->
                val weekStart = currentWeek.minusWeeks(weeksAgo.toLong())
                WeekBar(weekStart, countsByWeek[weekStart] ?: 0)
            }

            return ProgressStats(
                attended = attendedWorkouts.size,
                missed = history.count { it.status == BookingStatus.NO_SHOW },
                totalMinutes = attendedWorkouts.sumOf { it.durationMinutes },
                totalCalories = attendedWorkouts.sumOf { it.estimatedCalories },
                minutesByType = minutesByType,
                favoriteType = minutesByType.keys.firstOrNull(),
                thisWeek = countsByWeek[currentWeek] ?: 0,
                weeklyGoal = state.weeklyGoal,
                weekStreak = weekStreak(countsByWeek, currentWeek, state.weeklyGoal),
                lastWeeks = lastWeeks
            )
        }

        fun weekStreak(countsByWeek: Map<LocalDate, Int>, currentWeek: LocalDate, goal: Int): Int {
            val currentDone = (countsByWeek[currentWeek] ?: 0) >= goal
            var week = if (currentDone) currentWeek else currentWeek.minusWeeks(1)
            var streak = 0
            while ((countsByWeek[week] ?: 0) >= goal) {
                streak++
                week = week.minusWeeks(1)
            }
            return streak
        }
    }
}
