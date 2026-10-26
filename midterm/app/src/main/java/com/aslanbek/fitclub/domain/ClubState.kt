package com.aslanbek.fitclub.domain

import com.aslanbek.fitclub.model.Booking
import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.model.Member
import com.aslanbek.fitclub.model.Participation
import com.aslanbek.fitclub.model.Trainer
import com.aslanbek.fitclub.model.Workout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

data class ClubState(
    val now: LocalDateTime,
    val currentMemberId: Int,
    val members: List<Member>,
    val trainers: List<Trainer>,
    val workouts: List<Workout>,
    val bookings: List<Booking>,
    val waitlists: Map<Int, List<Int>> = emptyMap(),
    val weeklyGoal: Int = 3,
    val liveActivity: Boolean = true
) {
    private val workoutsById: Map<Int, Workout> by lazy { workouts.associateBy { it.id } }
    private val trainersById: Map<Int, Trainer> by lazy { trainers.associateBy { it.id } }
    private val takenSpotsByWorkout: Map<Int, Int> by lazy {
        bookings.filter { it.status.holdsSpot }.groupingBy { it.workoutId }.eachCount()
    }

    val today: LocalDate get() = now.toLocalDate()

    val currentMember: Member get() = members.first { it.id == currentMemberId }

    fun member(id: Int): Member? = members.find { it.id == id }

    fun workout(id: Int): Workout? = workoutsById[id]

    fun trainer(id: Int): Trainer? = trainersById[id]

    fun takenSpots(workoutId: Int): Int = takenSpotsByWorkout[workoutId] ?: 0

    fun spotsLeft(workout: Workout): Int = (workout.capacity - takenSpots(workout.id)).coerceAtLeast(0)

    fun activeBooking(memberId: Int, workoutId: Int): Booking? =
        bookings.find { it.memberId == memberId && it.workoutId == workoutId && it.status.holdsSpot }

    fun waitlistPosition(memberId: Int, workoutId: Int): Int? {
        val index = waitlists[workoutId].orEmpty().indexOf(memberId)
        return if (index >= 0) index + 1 else null
    }

    fun participation(workoutId: Int, memberId: Int = currentMemberId): Participation {
        val booking = activeBooking(memberId, workoutId)
        return when (booking?.status) {
            BookingStatus.BOOKED -> Participation.Booked
            BookingStatus.ATTENDED -> Participation.Attended
            BookingStatus.NO_SHOW -> Participation.Missed
            else -> waitlistPosition(memberId, workoutId)
                ?.let { Participation.Waitlisted(it) }
                ?: Participation.None
        }
    }

    fun bookingsOf(memberId: Int): List<Booking> = bookings.filter { it.memberId == memberId }

    fun strikesOf(memberId: Int): List<Booking> {
        val windowStart = now.minusDays(ClubRules.STRIKE_WINDOW_DAYS)
        return bookingsOf(memberId).filter { it.status.isStrike && it.updatedAt >= windowStart }
    }

    fun classesInWeek(memberId: Int, date: LocalDate): Int {
        val weekStart = date.startOfWeek()
        val weekEnd = weekStart.plusDays(7)
        return bookingsOf(memberId).count { booking ->
            val start = workout(booking.workoutId)?.start?.toLocalDate()
            booking.status.holdsSpot && start != null && start >= weekStart && start < weekEnd
        }
    }

    fun summaryOf(workout: Workout): WorkoutSummary = WorkoutSummary(
        workout = workout,
        trainerName = trainer(workout.trainerId)?.name ?: "Trainer",
        spotsLeft = spotsLeft(workout),
        waitlistSize = waitlists[workout.id].orEmpty().size,
        participation = participation(workout.id),
        hasStarted = now >= workout.start,
        hasEnded = now >= workout.end
    )
}

data class WorkoutSummary(
    val workout: Workout,
    val trainerName: String,
    val spotsLeft: Int,
    val waitlistSize: Int,
    val participation: Participation,
    val hasStarted: Boolean,
    val hasEnded: Boolean
) {
    val id: Int get() = workout.id
    val isFull: Boolean get() = spotsLeft == 0
    val takenSpots: Int get() = workout.capacity - spotsLeft
    val fillRatio: Float get() = takenSpots.toFloat() / workout.capacity
}

fun LocalDate.startOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
