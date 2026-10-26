package com.aslanbek.fitclub

import com.aslanbek.fitclub.data.InMemoryFitClubRepository
import com.aslanbek.fitclub.data.SampleData
import com.aslanbek.fitclub.domain.ClubRules
import com.aslanbek.fitclub.model.Membership
import com.aslanbek.fitclub.model.Participation
import com.aslanbek.fitclub.model.WorkoutType
import com.aslanbek.fitclub.ui.bookings.BookingsViewModel
import com.aslanbek.fitclub.ui.details.DetailsAction
import com.aslanbek.fitclub.ui.details.WorkoutDetailsViewModel
import com.aslanbek.fitclub.ui.progress.ProgressViewModel
import com.aslanbek.fitclub.ui.progress.RatingsLoad
import com.aslanbek.fitclub.ui.schedule.ScheduleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class UiStateTest {
    private val now = LocalDateTime.of(2026, 10, 28, 14, 7)
    private val club = SampleData.create(now)

    @Test
    fun `schedule shows seven days and filters the selected one`() {
        val all = ScheduleViewModel.buildState(club, dayOffset = 1, query = "", type = null, onlyAvailable = false)
        assertEquals(7, all.days.size)
        assertEquals(now.toLocalDate().plusDays(1), all.selectedDay)
        assertTrue(all.workouts.isNotEmpty())
        assertTrue(all.workouts.all { it.workout.start.toLocalDate() == all.selectedDay })
        assertEquals(all.workouts.sortedBy { it.workout.start }, all.workouts)

        val hiit = ScheduleViewModel.buildState(club, 1, "", WorkoutType.HIIT, false)
        assertTrue(hiit.workouts.all { it.workout.type == WorkoutType.HIIT })

        val byTrainer = ScheduleViewModel.buildState(club, 1, "aigerim", null, false)
        assertTrue(byTrainer.workouts.all { it.trainerName.startsWith("Aigerim") })

        val free = ScheduleViewModel.buildState(club, 2, "", null, true)
        assertTrue(free.workouts.none { it.isFull })
        assertEquals("Basic: 2 of 3 classes this week", all.membershipText)
    }

    @Test
    fun `today hides classes that already ended`() {
        val today = ScheduleViewModel.buildState(club, 0, "", null, false)
        assertTrue(today.workouts.all { it.workout.end > now })
        assertTrue(today.workouts.any { it.workout.title == "Core Blast" })
    }

    @Test
    fun `details offer the right action for each situation`() {
        val soon = club.workouts.first { it.title == "Core Blast" }
        val soonState = WorkoutDetailsViewModel.buildState(club, soon.id, null)
        assertEquals(DetailsAction.CHECK_IN, soonState.primaryAction)
        assertEquals(DetailsAction.CANCEL, soonState.secondaryAction)
        assertTrue(soonState.cancelIsLate)

        val waiting = club.workouts.first { club.participation(it.id) is Participation.Waitlisted }
        assertEquals(DetailsAction.LEAVE_WAITLIST, WorkoutDetailsViewModel.buildState(club, waiting.id, null).primaryAction)

        val open = club.workouts.first {
            it.start > now.plusDays(3) && club.spotsLeft(it) > 0 && club.participation(it.id) == Participation.None
        }
        val openState = WorkoutDetailsViewModel.buildState(club, open.id, null)
        assertEquals(DetailsAction.BOOK, openState.primaryAction)
        assertNull(openState.secondaryAction)

        val past = club.workouts.first { it.end < now && club.participation(it.id) == Participation.Attended }
        assertNull(WorkoutDetailsViewModel.buildState(club, past.id, null).primaryAction)

        assertTrue(WorkoutDetailsViewModel.buildState(club, -1, null).notFound)
    }

    @Test
    fun `bookings split into upcoming, waitlist and history`() {
        val state = BookingsViewModel.buildState(club)
        assertEquals(2, state.upcoming.size)
        assertEquals(1, state.waitlist.size)
        assertEquals(17, state.history.size)
        assertEquals("Core Blast", state.next?.summary?.workout?.title)
        assertTrue(state.next!!.canCheckIn)
        assertEquals(state.history.sortedByDescending { it.summary.workout.start }, state.history)
    }

    @Test
    fun `starts-in text is readable`() {
        assertEquals("Starts in 18 min", BookingsViewModel.startsInText(Duration.ofMinutes(18)))
        assertEquals("Starts in 2 h 5 min", BookingsViewModel.startsInText(Duration.ofMinutes(125)))
        assertEquals("Starts in 1 d 3 h", BookingsViewModel.startsInText(Duration.ofHours(27)))
        assertEquals("In progress", BookingsViewModel.startsInText(Duration.ofMinutes(-3)))
    }

    @Test
    fun `progress reacts to membership and strikes`() {
        val repository = InMemoryFitClubRepository(club)
        repository.setMembership(Membership.PREMIUM)
        val state = ProgressViewModel.buildState(repository.state.value, RatingsLoad())
        assertEquals(Membership.PREMIUM, state.membership)
        assertEquals(1, state.strikes)
        assertNull(state.blockedUntil)
        assertNotNull(state.stats)
        assertEquals(club.trainers.size, state.trainers.size)

        val soon = club.workouts.first { it.title == "Core Blast" }
        repository.cancel(soon.id)
        val blocked = repository.state.value.let { s ->
            s.copy(bookings = s.bookings + s.bookingsOf(SampleData.YOU).first { it.status.isStrike }.copy(id = 99999))
        }
        assertTrue(ClubRules.isBlocked(blocked, SampleData.YOU))
        assertNotNull(ProgressViewModel.buildState(blocked, RatingsLoad()).blockedUntil)
    }
}
