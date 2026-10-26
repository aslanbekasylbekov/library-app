package com.aslanbek.fitclub

import com.aslanbek.fitclub.domain.ClubRules
import com.aslanbek.fitclub.domain.ClubState
import com.aslanbek.fitclub.model.Booking
import com.aslanbek.fitclub.model.BookingResult
import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.model.CancelResult
import com.aslanbek.fitclub.model.CheckInResult
import com.aslanbek.fitclub.model.Intensity
import com.aslanbek.fitclub.model.Member
import com.aslanbek.fitclub.model.Membership
import com.aslanbek.fitclub.model.Participation
import com.aslanbek.fitclub.model.Trainer
import com.aslanbek.fitclub.model.Workout
import com.aslanbek.fitclub.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ClubRulesTest {
    private val monday = LocalDateTime.of(2026, 10, 26, 9, 0)

    private fun workout(id: Int, dayOffset: Long, hour: Int, capacity: Int = 2, duration: Int = 60) = Workout(
        id = id,
        title = "Class $id",
        type = WorkoutType.HIIT,
        intensity = Intensity.HIGH,
        trainerId = 1,
        room = "Room $id",
        start = monday.toLocalDate().plusDays(dayOffset).atTime(hour, 0),
        durationMinutes = duration,
        capacity = capacity,
        description = ""
    )

    private fun state(
        workouts: List<Workout>,
        bookings: List<Booking> = emptyList(),
        membership: Membership = Membership.BASIC,
        now: LocalDateTime = monday
    ) = ClubState(
        now = now,
        currentMemberId = 1,
        members = listOf(
            Member(1, "You", membership),
            Member(2, "Aliya", Membership.PREMIUM),
            Member(3, "Nurlan", Membership.PREMIUM)
        ),
        trainers = listOf(Trainer(1, "Coach", WorkoutType.HIIT, "")),
        workouts = workouts,
        bookings = bookings
    )

    @Test
    fun `books a class with free spots`() {
        val s = state(listOf(workout(1, 0, 18)))
        val outcome = ClubRules.book(s, 1, 1)
        assertTrue(outcome.result is BookingResult.Booked)
        assertEquals(Participation.Booked, outcome.state.participation(1))
        assertEquals(1, outcome.state.spotsLeft(outcome.state.workout(1)!!))
    }

    @Test
    fun `full class puts member on the waitlist`() {
        val bookings = listOf(
            Booking(1, 1, 2, BookingStatus.BOOKED, monday),
            Booking(2, 1, 3, BookingStatus.BOOKED, monday)
        )
        val outcome = ClubRules.book(state(listOf(workout(1, 0, 18)), bookings), 1, 1)
        assertEquals(BookingResult.Waitlisted(outcome.state.workout(1)!!, 1), outcome.result)
        assertEquals(Participation.Waitlisted(1), outcome.state.participation(1))
    }

    @Test
    fun `cancellation promotes the first member on the waitlist`() {
        val bookings = listOf(
            Booking(1, 1, 2, BookingStatus.BOOKED, monday),
            Booking(2, 1, 3, BookingStatus.BOOKED, monday)
        )
        val waiting = ClubRules.book(state(listOf(workout(1, 0, 18)), bookings), 1, 1).state
        val outcome = ClubRules.cancel(waiting, 2, 1)
        val result = outcome.result as CancelResult.Cancelled
        assertFalse(result.late)
        assertEquals(1, result.promoted?.id)
        assertEquals(Participation.Booked, outcome.state.participation(1))
        assertTrue(outcome.state.waitlists[1].orEmpty().isEmpty())
    }

    @Test
    fun `basic membership allows only three classes per week`() {
        val workouts = (1..4).map { workout(it, it.toLong(), 18) }
        var s = state(workouts)
        repeat(3) { i -> s = ClubRules.book(s, 1, i + 1).state }
        assertEquals(BookingResult.LimitReached(3), ClubRules.book(s, 1, 4).result)

        val premium = s.copy(members = s.members.map { if (it.id == 1) it.copy(membership = Membership.PREMIUM) else it })
        assertTrue(ClubRules.book(premium, 1, 4).result is BookingResult.Booked)
    }

    @Test
    fun `limit is counted per calendar week`() {
        val workouts = listOf(workout(1, 1, 18), workout(2, 2, 18), workout(3, 3, 18), workout(4, 7, 18))
        var s = state(workouts)
        repeat(3) { i -> s = ClubRules.book(s, 1, i + 1).state }
        assertTrue(ClubRules.book(s, 1, 4).result is BookingResult.Booked)
    }

    @Test
    fun `overlapping classes are rejected`() {
        val first = workout(1, 0, 18, duration = 60)
        val second = workout(2, 0, 18, duration = 45).copy(start = first.start.plusMinutes(30))
        val s = ClubRules.book(state(listOf(first, second)), 1, 1).state
        assertEquals(BookingResult.TimeConflict(first), ClubRules.book(s, 1, 2).result)
    }

    @Test
    fun `late cancellation is a strike and three strikes block booking`() {
        val soon = workout(1, 0, 10)
        val booked = ClubRules.book(state(listOf(soon, workout(2, 1, 18))), 1, 1).state
        val outcome = ClubRules.cancel(booked, 1, 1)
        assertTrue((outcome.result as CancelResult.Cancelled).late)
        assertEquals(1, outcome.state.strikesOf(1).size)

        val strikes = listOf(
            Booking(10, 1, 1, BookingStatus.NO_SHOW, monday.minusDays(3)),
            Booking(11, 1, 1, BookingStatus.NO_SHOW, monday.minusDays(5))
        )
        val blocked = outcome.state.copy(bookings = outcome.state.bookings + strikes)
        val result = ClubRules.book(blocked, 1, 2).result
        assertTrue(result is BookingResult.Blocked)
        assertEquals(monday.minusDays(5).plusDays(30).toLocalDate(), (result as BookingResult.Blocked).until)
    }

    @Test
    fun `old strikes expire after thirty days`() {
        val old = listOf(
            Booking(10, 99, 1, BookingStatus.NO_SHOW, monday.minusDays(40)),
            Booking(11, 99, 1, BookingStatus.NO_SHOW, monday.minusDays(41)),
            Booking(12, 99, 1, BookingStatus.LATE_CANCELLED, monday.minusDays(42))
        )
        assertTrue(ClubRules.book(state(listOf(workout(1, 1, 18)), old), 1, 1).result is BookingResult.Booked)
    }

    @Test
    fun `check-in opens thirty minutes before the start`() {
        val w = workout(1, 0, 18)
        val booked = ClubRules.book(state(listOf(w)), 1, 1).state
        assertTrue(ClubRules.checkIn(booked, 1, 1).result is CheckInResult.TooEarly)

        val atDoor = booked.copy(now = w.start.minusMinutes(20))
        val outcome = ClubRules.checkIn(atDoor, 1, 1)
        assertEquals(CheckInResult.CheckedIn(w), outcome.result)
        assertEquals(Participation.Attended, outcome.state.participation(1))
    }

    @Test
    fun `missed class becomes a no-show when time passes`() {
        val w = workout(1, 0, 10)
        val booked = ClubRules.book(state(listOf(w)), 1, 1).state
        val outcome = ClubRules.advanceTime(booked, w.end.plusMinutes(1))
        assertEquals(listOf(w), outcome.result.newNoShows)
        assertEquals(Participation.Missed, outcome.state.participation(1))
        assertEquals(1, outcome.state.strikesOf(1).size)
    }

    @Test
    fun `cannot book or cancel a class that already started`() {
        val w = workout(1, 0, 18)
        val booked = ClubRules.book(state(listOf(w)), 1, 1).state
        val started = booked.copy(now = w.start.plusMinutes(5))
        assertEquals(CancelResult.AlreadyStarted(w), ClubRules.cancel(started, 1, 1).result)
        assertEquals(BookingResult.AlreadyStarted(w), ClubRules.book(started, 2, 1).result)
    }

    @Test
    fun `promotion skips members who are no longer eligible`() {
        val target = workout(1, 1, 18, capacity = 1)
        val clash = workout(2, 1, 18, capacity = 5).copy(room = "Other")
        val bookings = listOf(Booking(1, 1, 2, BookingStatus.BOOKED, monday))
        var s = state(listOf(target, clash), bookings)
        s = ClubRules.book(s, 1, 1).state
        s = ClubRules.book(s, 3, 1).state
        s = ClubRules.book(s, 1, 2).state

        val outcome = ClubRules.cancel(s, 2, 1)
        assertEquals(3, (outcome.result as CancelResult.Cancelled).promoted?.id)
        assertEquals(Participation.None, outcome.state.participation(1))
    }
}
