package com.aslanbek.fitclub.domain

import com.aslanbek.fitclub.model.Intensity
import com.aslanbek.fitclub.model.Workout
import com.aslanbek.fitclub.model.WorkoutType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class NewWorkoutInput(
    val title: String,
    val type: WorkoutType,
    val intensity: Intensity,
    val trainerId: Int?,
    val date: LocalDate,
    val time: String,
    val duration: String,
    val capacity: String,
    val room: String,
    val description: String
)

enum class WorkoutField { TITLE, TRAINER, TIME, DURATION, CAPACITY, ROOM }

sealed class WorkoutValidation {
    data class Valid(val workout: Workout) : WorkoutValidation()
    data class Invalid(val field: WorkoutField, val message: String) : WorkoutValidation()
}

object WorkoutValidator {
    val DURATION_RANGE = 15..180
    val CAPACITY_RANGE = 1..40
    private val TIME_PATTERN = Regex("""^([01]?\d|2[0-3]):([0-5]\d)$""")

    fun parseTime(text: String): LocalTime? {
        val match = TIME_PATTERN.matchEntire(text.trim()) ?: return null
        val (hours, minutes) = match.destructured
        return LocalTime.of(hours.toInt(), minutes.toInt())
    }

    fun validate(input: NewWorkoutInput, now: LocalDateTime, existing: List<Workout>): WorkoutValidation {
        val title = input.title.trim()
        if (title.length < 3) return WorkoutValidation.Invalid(WorkoutField.TITLE, "Title must have at least 3 characters")
        if (input.trainerId == null) return WorkoutValidation.Invalid(WorkoutField.TRAINER, "Choose a trainer")

        val time = parseTime(input.time)
            ?: return WorkoutValidation.Invalid(WorkoutField.TIME, "Enter time as HH:mm, for example 18:30")
        val start = LocalDateTime.of(input.date, time)
        if (start <= now) return WorkoutValidation.Invalid(WorkoutField.TIME, "The class must start in the future")

        val duration = input.duration.trim().toIntOrNull()
        if (duration == null || duration !in DURATION_RANGE) {
            return WorkoutValidation.Invalid(
                WorkoutField.DURATION,
                "Duration must be ${DURATION_RANGE.first}–${DURATION_RANGE.last} minutes"
            )
        }
        val capacity = input.capacity.trim().toIntOrNull()
        if (capacity == null || capacity !in CAPACITY_RANGE) {
            return WorkoutValidation.Invalid(
                WorkoutField.CAPACITY,
                "Capacity must be ${CAPACITY_RANGE.first}–${CAPACITY_RANGE.last} people"
            )
        }
        val room = input.room.trim()
        if (room.isEmpty()) return WorkoutValidation.Invalid(WorkoutField.ROOM, "Enter a room")

        val workout = Workout(
            id = 0,
            title = title,
            type = input.type,
            intensity = input.intensity,
            trainerId = input.trainerId,
            room = room,
            start = start,
            durationMinutes = duration,
            capacity = capacity,
            description = input.description.trim().ifEmpty { "${input.type.label} class, ${input.intensity.label.lowercase()} intensity." }
        )
        val overlapping = existing.filter { it.overlaps(workout) }
        overlapping.firstOrNull { it.room.equals(room, ignoreCase = true) }?.let { other ->
            return WorkoutValidation.Invalid(WorkoutField.ROOM, "$room is busy: \"${other.title}\" overlaps this time")
        }
        overlapping.firstOrNull { it.trainerId == workout.trainerId }?.let { other ->
            return WorkoutValidation.Invalid(WorkoutField.TRAINER, "This trainer already leads \"${other.title}\" at this time")
        }
        return WorkoutValidation.Valid(workout)
    }
}
