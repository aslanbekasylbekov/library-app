package com.aslanbek.fitclub.model

import java.time.LocalDate
import java.time.LocalDateTime

sealed class BookingResult {
    data class Booked(val workout: Workout) : BookingResult()
    data class Waitlisted(val workout: Workout, val position: Int) : BookingResult()
    data class AlreadyBooked(val workout: Workout) : BookingResult()
    data class AlreadyWaitlisted(val workout: Workout, val position: Int) : BookingResult()
    data class LimitReached(val limit: Int) : BookingResult()
    data class TimeConflict(val other: Workout) : BookingResult()
    data class Blocked(val strikes: Int, val until: LocalDate) : BookingResult()
    data class AlreadyStarted(val workout: Workout) : BookingResult()
    data object WorkoutNotFound : BookingResult()
}

sealed class CancelResult {
    data class Cancelled(
        val workout: Workout,
        val late: Boolean,
        val promoted: Member?
    ) : CancelResult()
    data class AlreadyStarted(val workout: Workout) : CancelResult()
    data object NotBooked : CancelResult()
    data object WorkoutNotFound : CancelResult()
}

sealed class CheckInResult {
    data class CheckedIn(val workout: Workout) : CheckInResult()
    data class TooEarly(val opensAt: LocalDateTime) : CheckInResult()
    data object Finished : CheckInResult()
    data object NotBooked : CheckInResult()
    data object WorkoutNotFound : CheckInResult()
}

sealed interface Participation {
    data object None : Participation
    data object Booked : Participation
    data object Attended : Participation
    data object Missed : Participation
    data class Waitlisted(val position: Int) : Participation
}
