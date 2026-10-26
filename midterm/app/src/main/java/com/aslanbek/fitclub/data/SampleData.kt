package com.aslanbek.fitclub.data

import com.aslanbek.fitclub.domain.ClubState
import com.aslanbek.fitclub.domain.startOfWeek
import com.aslanbek.fitclub.model.Booking
import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.model.Intensity
import com.aslanbek.fitclub.model.Member
import com.aslanbek.fitclub.model.Membership
import com.aslanbek.fitclub.model.Trainer
import com.aslanbek.fitclub.model.Workout
import com.aslanbek.fitclub.model.WorkoutType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

object SampleData {
    const val YOU = 1
    private const val DAYS_AHEAD = 7L
    private const val WEEKS_OF_HISTORY = 5L

    val trainers = listOf(
        Trainer(1, "Aigerim Sadykova", WorkoutType.YOGA, "Hatha and vinyasa teacher, 8 years of practice. Focus on breathing and mobility."),
        Trainer(2, "Daniyar Kenzhebayev", WorkoutType.HIIT, "Former track athlete. Short, intense intervals with clear coaching."),
        Trainer(3, "Madina Tulegenova", WorkoutType.CYCLING, "Indoor cycling coach who builds every ride around music and heart-rate zones."),
        Trainer(4, "Arman Bekov", WorkoutType.STRENGTH, "Certified strength coach. Teaches safe technique with free weights."),
        Trainer(5, "Saule Zhakupova", WorkoutType.SWIMMING, "Master of sports in swimming. Helps adults fix technique and gain endurance."),
        Trainer(6, "Dana Omarova", WorkoutType.YOGA, "Stretching and recovery classes for people who sit at a desk all day.")
    )

    val members = listOf(
        Member(YOU, "Aslanbek", Membership.BASIC),
        Member(2, "Aliya", Membership.PREMIUM),
        Member(3, "Nurlan", Membership.BASIC),
        Member(4, "Kamila", Membership.PREMIUM),
        Member(5, "Yerlan", Membership.BASIC),
        Member(6, "Zarina", Membership.PREMIUM),
        Member(7, "Asel", Membership.PREMIUM),
        Member(8, "Timur", Membership.BASIC),
        Member(9, "Dinara", Membership.PREMIUM),
        Member(10, "Ruslan", Membership.PREMIUM),
        Member(11, "Gulnaz", Membership.BASIC),
        Member(12, "Marat", Membership.PREMIUM),
        Member(13, "Aruzhan", Membership.PREMIUM),
        Member(14, "Bekzat", Membership.PREMIUM),
        Member(15, "Togzhan", Membership.PREMIUM),
        Member(16, "Ilyas", Membership.PREMIUM)
    )

    private data class Template(
        val title: String,
        val type: WorkoutType,
        val intensity: Intensity,
        val trainerId: Int,
        val room: String,
        val time: LocalTime,
        val duration: Int,
        val capacity: Int,
        val description: String
    )

    private val templates = listOf(
        Template("Morning Flow Yoga", WorkoutType.YOGA, Intensity.LOW, 1, "Studio A", LocalTime.of(7, 0), 60, 14,
            "A calm vinyasa flow to wake up the body. Suitable for beginners, mats are provided."),
        Template("Spin Express", WorkoutType.CYCLING, Intensity.HIGH, 3, "Cycle Room", LocalTime.of(8, 0), 45, 12,
            "Fast-paced ride with sprints and climbs. Bring a towel and water."),
        Template("Lunch HIIT", WorkoutType.HIIT, Intensity.HIGH, 2, "Studio B", LocalTime.of(12, 30), 30, 10,
            "30 minutes of intervals: 40 seconds of work, 20 seconds of rest. Perfect for a lunch break."),
        Template("Swim Technique", WorkoutType.SWIMMING, Intensity.MEDIUM, 5, "Pool", LocalTime.of(17, 0), 50, 8,
            "Freestyle and backstroke drills in small groups. You need to be able to swim 50 m."),
        Template("Strength Basics", WorkoutType.STRENGTH, Intensity.MEDIUM, 4, "Weights Zone", LocalTime.of(18, 0), 60, 10,
            "Squat, deadlift and press technique with light weights. Great first strength class."),
        Template("Power HIIT", WorkoutType.HIIT, Intensity.HIGH, 2, "Studio B", LocalTime.of(19, 0), 45, 12,
            "Full-body intervals with kettlebells and jumps. High heart rate guaranteed."),
        Template("Evening Stretch", WorkoutType.YOGA, Intensity.LOW, 6, "Studio A", LocalTime.of(20, 30), 45, 16,
            "Slow stretching for the back, hips and shoulders. Ideal after a day at the desk."),
        Template("Endurance Ride", WorkoutType.CYCLING, Intensity.MEDIUM, 3, "Cycle Room", LocalTime.of(19, 30), 60, 12,
            "Steady ride in heart-rate zones 2 and 3 to build endurance."),
        Template("Aqua Fitness", WorkoutType.SWIMMING, Intensity.LOW, 5, "Pool", LocalTime.of(10, 0), 45, 10,
            "Low-impact workout in the water. Easy on the joints, good for all levels."),
        Template("Full Body Strength", WorkoutType.STRENGTH, Intensity.HIGH, 4, "Weights Zone", LocalTime.of(7, 30), 60, 8,
            "Heavy compound lifts in small groups. For members who already know the basic technique.")
    )

    private val weeklyPlan: Map<DayOfWeek, List<Int>> = mapOf(
        DayOfWeek.MONDAY to listOf(0, 1, 2, 4, 5, 6),
        DayOfWeek.TUESDAY to listOf(9, 8, 2, 3, 7, 6),
        DayOfWeek.WEDNESDAY to listOf(0, 1, 2, 4, 5, 6),
        DayOfWeek.THURSDAY to listOf(9, 8, 2, 3, 7, 6),
        DayOfWeek.FRIDAY to listOf(0, 1, 2, 4, 5),
        DayOfWeek.SATURDAY to listOf(8, 0, 4, 7),
        DayOfWeek.SUNDAY to listOf(8, 3, 6)
    )

    private val weeklyAttendance = listOf(4, 3, 2, 4, 3)

    fun create(now: LocalDateTime, seed: Int = 42): ClubState {
        val random = Random(seed)
        val today = now.toLocalDate()
        val firstDay = today.startOfWeek().minusWeeks(WEEKS_OF_HISTORY)
        val lastDay = today.plusDays(DAYS_AHEAD)

        val workouts = mutableListOf<Workout>()
        var date = firstDay
        while (date < lastDay) {
            weeklyPlan.getValue(date.dayOfWeek).forEach { index ->
                val t = templates[index]
                workouts += Workout(
                    id = workouts.size + 1,
                    title = t.title,
                    type = t.type,
                    intensity = t.intensity,
                    trainerId = t.trainerId,
                    room = t.room,
                    start = LocalDateTime.of(date, t.time),
                    durationMinutes = t.duration,
                    capacity = t.capacity,
                    description = t.description
                )
            }
            date = date.plusDays(1)
        }

        val startingSoon = Workout(
            id = workouts.size + 1,
            title = "Core Blast",
            type = WorkoutType.HIIT,
            intensity = Intensity.MEDIUM,
            trainerId = 2,
            room = "Studio C",
            start = now.truncatedTo(ChronoUnit.HOURS).plusMinutes(((now.minute / 5) + 1) * 5L + 15),
            durationMinutes = 30,
            capacity = 10,
            description = "Short core workout: planks, hollow holds and rotations. Check in at the front desk up to 30 minutes before the start."
        )
        workouts += startingSoon
        workouts.sortBy { it.start }

        val bookings = mutableListOf<Booking>()
        fun addBooking(workout: Workout, memberId: Int, status: BookingStatus) {
            bookings += Booking(bookings.size + 1, workout.id, memberId, status, minOf(workout.start, now))
        }

        val yourHistory = pickYourHistory(workouts, today, random)
        yourHistory.forEach { addBooking(it, YOU, BookingStatus.ATTENDED) }
        val missed = workouts
            .filter { candidate ->
                candidate.start.toLocalDate() == today.startOfWeek().minusDays(3) &&
                    yourHistory.none { it == candidate || it.overlaps(candidate) }
            }
            .let { sameDay -> sameDay.firstOrNull { it.type == WorkoutType.CYCLING } ?: sameDay.firstOrNull() }
        if (missed != null) addBooking(missed, YOU, BookingStatus.NO_SHOW)

        val tomorrowEvening = workouts.firstOrNull {
            it.start.toLocalDate() == today.plusDays(1) && it.start.hour >= 17
        }
        val fullClass = workouts.filter { it.start.toLocalDate() == today.plusDays(2) }
            .let { dayAfterTomorrow -> dayAfterTomorrow.firstOrNull { it.type == WorkoutType.HIIT } ?: dayAfterTomorrow.lastOrNull() }
        val upcomingForYou = listOfNotNull(startingSoon, tomorrowEvening)
        upcomingForYou.forEach { addBooking(it, YOU, BookingStatus.BOOKED) }

        val others = members.filter { it.id != YOU }
        val waitlists = mutableMapOf<Int, List<Int>>()
        workouts.forEach { workout ->
            val taken = bookings.count { it.workoutId == workout.id }
            val target = when {
                workout == fullClass -> workout.capacity
                workout == startingSoon -> workout.capacity - 3
                workout.start < now -> (workout.capacity * random.nextDouble(0.5, 1.0)).toInt()
                workout.start.toLocalDate() == today.plusDays(1) -> (workout.capacity * random.nextDouble(0.6, 1.0)).toInt()
                else -> (workout.capacity * random.nextDouble(0.1, 0.8)).toInt()
            }
            val status = if (workout.end <= now) BookingStatus.ATTENDED else BookingStatus.BOOKED
            others.shuffled(random).take((target - taken).coerceAtLeast(0)).forEach {
                addBooking(workout, it.id, status)
            }
        }
        if (fullClass != null) {
            val booked = bookings.filter { it.workoutId == fullClass.id }.map { it.memberId }.toSet()
            waitlists[fullClass.id] = listOf(YOU) + others.map { it.id }.filter { it !in booked }.take(1)
        }

        return ClubState(
            now = now,
            currentMemberId = YOU,
            members = members,
            trainers = trainers,
            workouts = workouts,
            bookings = bookings,
            waitlists = waitlists
        )
    }

    private fun pickYourHistory(workouts: List<Workout>, today: LocalDate, random: Random): List<Workout> {
        val thisWeek = today.startOfWeek()
        return weeklyAttendance.flatMapIndexed { index, count ->
            val weekStart = thisWeek.minusWeeks((weeklyAttendance.size - index).toLong())
            workouts
                .filter { it.start.toLocalDate() >= weekStart && it.start.toLocalDate() < weekStart.plusDays(7) }
                .groupBy { it.start.toLocalDate() }
                .values
                .shuffled(random)
                .take(count)
                .map { sameDay -> sameDay.random(random) }
        }
    }

    fun upcomingDays(today: LocalDate): List<LocalDate> =
        (0 until DAYS_AHEAD).map { today.plusDays(it) }
}
