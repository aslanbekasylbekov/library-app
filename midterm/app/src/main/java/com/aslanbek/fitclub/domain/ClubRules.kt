package com.aslanbek.fitclub.domain

import com.aslanbek.fitclub.model.Booking
import com.aslanbek.fitclub.model.BookingResult
import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.model.CancelResult
import com.aslanbek.fitclub.model.CheckInResult
import com.aslanbek.fitclub.model.Member
import com.aslanbek.fitclub.model.Workout
import java.time.Duration
import java.time.LocalDateTime

data class Outcome<out T>(val state: ClubState, val result: T)

data class SettleReport(val newNoShows: List<Workout>)

object ClubRules {
    const val FREE_CANCEL_HOURS = 2L
    const val STRIKE_LIMIT = 3
    const val STRIKE_WINDOW_DAYS = 30L
    const val CHECK_IN_OPENS_MINUTES = 30L

    fun book(state: ClubState, memberId: Int, workoutId: Int): Outcome<BookingResult> {
        val workout = state.workout(workoutId)
            ?: return Outcome(state, BookingResult.WorkoutNotFound)
        val member = state.member(memberId)
            ?: return Outcome(state, BookingResult.WorkoutNotFound)

        if (state.activeBooking(memberId, workoutId) != null) {
            return Outcome(state, BookingResult.AlreadyBooked(workout))
        }
        state.waitlistPosition(memberId, workoutId)?.let { position ->
            return Outcome(state, BookingResult.AlreadyWaitlisted(workout, position))
        }
        checkEligibility(state, member, workout)?.let { problem -> return Outcome(state, problem) }

        return if (state.spotsLeft(workout) > 0) {
            Outcome(state.withNewBooking(workoutId, memberId), BookingResult.Booked(workout))
        } else {
            val queue = state.waitlists[workoutId].orEmpty() + memberId
            Outcome(
                state.copy(waitlists = state.waitlists + (workoutId to queue)),
                BookingResult.Waitlisted(workout, queue.size)
            )
        }
    }

    fun cancel(state: ClubState, memberId: Int, workoutId: Int): Outcome<CancelResult> {
        val workout = state.workout(workoutId)
            ?: return Outcome(state, CancelResult.WorkoutNotFound)
        val booking = state.activeBooking(memberId, workoutId)
        if (booking == null || booking.status != BookingStatus.BOOKED) {
            return Outcome(state, CancelResult.NotBooked)
        }
        if (state.now >= workout.start) return Outcome(state, CancelResult.AlreadyStarted(workout))

        val late = isLateCancellation(state.now, workout)
        val newStatus = if (late) BookingStatus.LATE_CANCELLED else BookingStatus.CANCELLED
        val afterCancel = state.copy(
            bookings = state.bookings.map {
                if (it.id == booking.id) it.copy(status = newStatus, updatedAt = state.now) else it
            }
        )
        val promotion = promoteFromWaitlist(afterCancel, workoutId)
        return Outcome(
            promotion.state,
            CancelResult.Cancelled(workout, late, promotion.result.firstOrNull())
        )
    }

    fun leaveWaitlist(state: ClubState, memberId: Int, workoutId: Int): Outcome<Boolean> {
        val queue = state.waitlists[workoutId].orEmpty()
        if (memberId !in queue) return Outcome(state, false)
        val newQueue = queue - memberId
        return Outcome(state.copy(waitlists = state.waitlists + (workoutId to newQueue)), true)
    }

    fun checkIn(state: ClubState, memberId: Int, workoutId: Int): Outcome<CheckInResult> {
        val workout = state.workout(workoutId)
            ?: return Outcome(state, CheckInResult.WorkoutNotFound)
        val booking = state.activeBooking(memberId, workoutId)
        if (booking == null || booking.status != BookingStatus.BOOKED) {
            return Outcome(state, CheckInResult.NotBooked)
        }
        val opensAt = checkInOpensAt(workout)
        return when {
            state.now < opensAt -> Outcome(state, CheckInResult.TooEarly(opensAt))
            state.now >= workout.end -> Outcome(state, CheckInResult.Finished)
            else -> Outcome(
                state.copy(bookings = state.bookings.map {
                    if (it.id == booking.id) it.copy(status = BookingStatus.ATTENDED, updatedAt = state.now) else it
                }),
                CheckInResult.CheckedIn(workout)
            )
        }
    }

    fun advanceTime(state: ClubState, now: LocalDateTime): Outcome<SettleReport> =
        settle(state.copy(now = now))

    fun settle(state: ClubState): Outcome<SettleReport> {
        val missedByCurrentMember = mutableListOf<Workout>()
        val bookings = state.bookings.map { booking ->
            val workout = state.workout(booking.workoutId)
            if (booking.status == BookingStatus.BOOKED && workout != null && state.now >= workout.end) {
                if (booking.memberId == state.currentMemberId) missedByCurrentMember += workout
                booking.copy(status = BookingStatus.NO_SHOW, updatedAt = workout.end)
            } else {
                booking
            }
        }
        val waitlists = state.waitlists.filterKeys { workoutId ->
            val workout = state.workout(workoutId)
            workout != null && state.now < workout.start
        }
        return Outcome(state.copy(bookings = bookings, waitlists = waitlists), SettleReport(missedByCurrentMember))
    }

    fun addWorkout(state: ClubState, workout: Workout): Outcome<Workout> {
        val id = (state.workouts.maxOfOrNull { it.id } ?: 0) + 1
        val created = workout.copy(id = id)
        val workouts = (state.workouts + created).sortedBy { it.start }
        return Outcome(state.copy(workouts = workouts), created)
    }

    fun isLateCancellation(now: LocalDateTime, workout: Workout): Boolean =
        Duration.between(now, workout.start) < Duration.ofHours(FREE_CANCEL_HOURS)

    fun freeCancellationUntil(workout: Workout): LocalDateTime = workout.start.minusHours(FREE_CANCEL_HOURS)

    fun checkInOpensAt(workout: Workout): LocalDateTime = workout.start.minusMinutes(CHECK_IN_OPENS_MINUTES)

    fun isBlocked(state: ClubState, memberId: Int): Boolean =
        state.strikesOf(memberId).size >= STRIKE_LIMIT

    internal fun checkEligibility(state: ClubState, member: Member, workout: Workout): BookingResult? {
        if (state.now >= workout.start) return BookingResult.AlreadyStarted(workout)

        val strikes = state.strikesOf(member.id)
        if (strikes.size >= STRIKE_LIMIT) {
            val oldest = strikes.minOf { it.updatedAt }
            return BookingResult.Blocked(strikes.size, oldest.plusDays(STRIKE_WINDOW_DAYS).toLocalDate())
        }

        val limit = member.membership.weeklyLimit
        if (limit != null && state.classesInWeek(member.id, workout.start.toLocalDate()) >= limit) {
            return BookingResult.LimitReached(limit)
        }

        val conflict = state.bookingsOf(member.id)
            .filter { it.status == BookingStatus.BOOKED && it.workoutId != workout.id }
            .mapNotNull { state.workout(it.workoutId) }
            .firstOrNull { it.overlaps(workout) }
        if (conflict != null) return BookingResult.TimeConflict(conflict)

        return null
    }

    internal fun promoteFromWaitlist(state: ClubState, workoutId: Int): Outcome<List<Member>> {
        val workout = state.workout(workoutId) ?: return Outcome(state, emptyList())
        var current = state
        val promoted = mutableListOf<Member>()
        val queue = state.waitlists[workoutId].orEmpty().toMutableList()

        while (queue.isNotEmpty() && current.spotsLeft(workout) > 0) {
            val member = current.member(queue.removeAt(0)) ?: continue
            if (checkEligibility(current, member, workout) == null) {
                current = current.withNewBooking(workoutId, member.id)
                promoted += member
            }
        }
        current = current.copy(waitlists = current.waitlists + (workoutId to queue.toList()))
        return Outcome(current, promoted)
    }

    private fun ClubState.withNewBooking(workoutId: Int, memberId: Int): ClubState {
        val id = (bookings.maxOfOrNull { it.id } ?: 0) + 1
        return copy(bookings = bookings + Booking(id, workoutId, memberId, BookingStatus.BOOKED, now))
    }
}
