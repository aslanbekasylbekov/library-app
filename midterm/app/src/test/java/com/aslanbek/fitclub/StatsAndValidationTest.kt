package com.aslanbek.fitclub

import com.aslanbek.fitclub.data.InMemoryFitClubRepository
import com.aslanbek.fitclub.data.SampleData
import com.aslanbek.fitclub.domain.NewWorkoutInput
import com.aslanbek.fitclub.domain.ProgressStats
import com.aslanbek.fitclub.domain.WorkoutField
import com.aslanbek.fitclub.domain.WorkoutValidation
import com.aslanbek.fitclub.domain.WorkoutValidator
import com.aslanbek.fitclub.domain.startOfWeek
import com.aslanbek.fitclub.model.BookingResult
import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.model.CheckInResult
import com.aslanbek.fitclub.model.Intensity
import com.aslanbek.fitclub.model.Participation
import com.aslanbek.fitclub.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

class StatsAndValidationTest {
    private val now = LocalDateTime.of(2026, 10, 28, 14, 7)

    @Test
    fun `week streak counts consecutive weeks that met the goal`() {
        val current = LocalDate.of(2026, 10, 26)
        val counts = mapOf(
            current.minusWeeks(1) to 3,
            current.minusWeeks(2) to 4,
            current.minusWeeks(3) to 1,
            current.minusWeeks(4) to 5
        )
        assertEquals(2, ProgressStats.weekStreak(counts, current, goal = 3))
        assertEquals(3, ProgressStats.weekStreak(counts + (current to 3), current, goal = 3))
    }

    @Test
    fun `sample data gives a believable starting point`() {
        val state = SampleData.create(now)
        val stats = ProgressStats.from(state)
        assertEquals(16, stats.attended)
        assertEquals(1, stats.missed)
        assertEquals(1, state.strikesOf(SampleData.YOU).size)
        assertEquals(ProgressStats.WEEKS_IN_CHART, stats.lastWeeks.size)
        assertEquals(listOf(4, 3, 2, 4, 3, 0), stats.lastWeeks.map { it.workouts })
        assertEquals(2, stats.weekStreak)
        assertTrue(stats.minutesByType.isNotEmpty())

        val youWait = state.workouts.count { state.participation(it.id) is Participation.Waitlisted }
        assertEquals(1, youWait)
        val upcoming = state.bookingsOf(SampleData.YOU).count { it.status == BookingStatus.BOOKED }
        assertEquals(2, upcoming)
        assertTrue(state.workouts.all { it.capacity >= state.takenSpots(it.id) })
    }

    @Test
    fun `starting-soon class can be checked in right away`() {
        val repository = InMemoryFitClubRepository(SampleData.create(now))
        val soon = repository.state.value.workouts.first { it.title == "Core Blast" }
        assertTrue(soon.start > now && soon.start <= now.plusMinutes(30))
        assertTrue(repository.checkIn(soon.id) is CheckInResult.CheckedIn)
    }

    @Test
    fun `third class this week hits the basic limit`() {
        val repository = InMemoryFitClubRepository(SampleData.create(now))
        val state = repository.state.value
        val weekEnd = now.toLocalDate().startOfWeek().plusDays(7)
        val candidates = state.workouts.filter {
            it.start > now.plusHours(3) && it.start.toLocalDate() < weekEnd &&
                state.spotsLeft(it) > 0 && state.participation(it.id) == Participation.None
        }
        val results = candidates.map { repository.book(it.id) }
        assertTrue(results.any { it is BookingResult.Booked })
        assertTrue(results.any { it is BookingResult.LimitReached })
    }

    @Test
    fun `simulation keeps the club consistent`() {
        val repository = InMemoryFitClubRepository(SampleData.create(now))
        val random = Random(1)
        repeat(500) { repository.simulateOtherMembers(random) }
        val state = repository.state.value
        assertTrue(state.workouts.all { state.takenSpots(it.id) <= it.capacity })
        state.waitlists.forEach { (workoutId, queue) ->
            assertEquals(queue.size, queue.toSet().size)
            queue.forEach { memberId -> assertEquals(null, state.activeBooking(memberId, workoutId)) }
        }
    }

    private fun input(
        title: String = "Sunset Yoga",
        time: String = "21:30",
        duration: String = "45",
        capacity: String = "12",
        room: String = "Roof",
        trainerId: Int? = 1
    ) = NewWorkoutInput(
        title = title,
        type = WorkoutType.YOGA,
        intensity = Intensity.LOW,
        trainerId = trainerId,
        date = now.toLocalDate(),
        time = time,
        duration = duration,
        capacity = capacity,
        room = room,
        description = ""
    )

    @Test
    fun `validation reports the first invalid field`() {
        val existing = SampleData.create(now).workouts
        fun fieldOf(i: NewWorkoutInput) = (WorkoutValidator.validate(i, now, existing) as? WorkoutValidation.Invalid)?.field

        assertEquals(WorkoutField.TITLE, fieldOf(input(title = "Yo")))
        assertEquals(WorkoutField.TRAINER, fieldOf(input(trainerId = null)))
        assertEquals(WorkoutField.TIME, fieldOf(input(time = "25:00")))
        assertEquals(WorkoutField.TIME, fieldOf(input(time = "09:00")))
        assertEquals(WorkoutField.DURATION, fieldOf(input(duration = "5")))
        assertEquals(WorkoutField.CAPACITY, fieldOf(input(capacity = "abc")))
        assertEquals(WorkoutField.ROOM, fieldOf(input(room = "  ")))
        assertEquals(WorkoutField.ROOM, fieldOf(input(room = "studio a", time = "20:45")))
        assertEquals(WorkoutField.TRAINER, fieldOf(input(trainerId = 6, time = "20:45")))

        val valid = WorkoutValidator.validate(input(), now, existing)
        assertTrue(valid is WorkoutValidation.Valid)
        assertNotNull((valid as WorkoutValidation.Valid).workout.description.takeIf { it.isNotBlank() })
    }

    @Test
    fun `time parser accepts short hours`() {
        assertEquals(java.time.LocalTime.of(7, 5), WorkoutValidator.parseTime("7:05"))
        assertEquals(null, WorkoutValidator.parseTime("7.05"))
    }
}
