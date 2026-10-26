package com.aslanbek.fitclub.model

import java.time.LocalDateTime

enum class WorkoutType(val label: String) {
    YOGA("Yoga"),
    HIIT("HIIT"),
    CYCLING("Cycling"),
    STRENGTH("Strength"),
    SWIMMING("Swimming")
}

enum class Intensity(val label: String, val kcalPerMinute: Int) {
    LOW("Low", 4),
    MEDIUM("Medium", 7),
    HIGH("High", 10)
}

enum class Membership(val label: String, val weeklyLimit: Int?) {
    BASIC("Basic", 3),
    PREMIUM("Premium", null);

    val limitText: String
        get() = weeklyLimit?.let { "$it classes per week" } ?: "Unlimited classes"
}

data class Trainer(
    val id: Int,
    val name: String,
    val specialty: WorkoutType,
    val bio: String
)

data class Member(
    val id: Int,
    val name: String,
    val membership: Membership
)

data class Workout(
    val id: Int,
    val title: String,
    val type: WorkoutType,
    val intensity: Intensity,
    val trainerId: Int,
    val room: String,
    val start: LocalDateTime,
    val durationMinutes: Int,
    val capacity: Int,
    val description: String
) {
    val end: LocalDateTime get() = start.plusMinutes(durationMinutes.toLong())

    val estimatedCalories: Int get() = durationMinutes * intensity.kcalPerMinute

    fun overlaps(other: Workout): Boolean = start < other.end && other.start < end
}

enum class BookingStatus(val label: String) {
    BOOKED("Booked"),
    ATTENDED("Attended"),
    NO_SHOW("No-show"),
    CANCELLED("Cancelled"),
    LATE_CANCELLED("Late cancel");

    val holdsSpot: Boolean get() = this == BOOKED || this == ATTENDED || this == NO_SHOW

    val isStrike: Boolean get() = this == NO_SHOW || this == LATE_CANCELLED
}

data class Booking(
    val id: Int,
    val workoutId: Int,
    val memberId: Int,
    val status: BookingStatus,
    val updatedAt: LocalDateTime
)

data class TrainerRating(
    val trainerId: Int,
    val average: Double,
    val reviews: Int
)
