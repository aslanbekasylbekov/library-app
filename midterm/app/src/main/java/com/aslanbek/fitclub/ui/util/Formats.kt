package com.aslanbek.fitclub.ui.util

import com.aslanbek.fitclub.model.Workout
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formats {
    private val time = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val weekday = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
    private val shortDate = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val longDate = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    fun time(dateTime: LocalDateTime): String = dateTime.format(time)

    fun timeRange(workout: Workout): String = "${time(workout.start)}–${time(workout.end)}"

    fun weekday(date: LocalDate): String = date.format(weekday)

    fun dayMonth(date: LocalDate): String = date.format(dayMonth)

    fun relativeDay(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(shortDate)
    }

    fun longDate(date: LocalDate): String = date.format(longDate)

    fun whenText(workout: Workout, today: LocalDate): String =
        "${relativeDay(workout.start.toLocalDate(), today)}, ${timeRange(workout)}"
}
