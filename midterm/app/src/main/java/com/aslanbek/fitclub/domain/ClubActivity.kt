package com.aslanbek.fitclub.domain

import com.aslanbek.fitclub.model.BookingResult
import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.model.CancelResult
import com.aslanbek.fitclub.model.Workout
import kotlin.random.Random

sealed class ActivityEvent {
    data class OtherMemberCancelled(
        val workout: Workout,
        val youWerePromoted: Boolean,
        val youWereSkipped: Boolean
    ) : ActivityEvent()
    data class OtherMemberBooked(val workout: Workout) : ActivityEvent()
}

object ClubActivity {
    private const val HORIZON_DAYS = 3L

    fun simulateOtherMember(state: ClubState, random: Random): Outcome<ActivityEvent?> {
        val upcoming = state.workouts.filter {
            it.start > state.now.plusMinutes(ClubRules.CHECK_IN_OPENS_MINUTES) &&
                it.start < state.now.plusDays(HORIZON_DAYS)
        }
        if (upcoming.isEmpty()) return Outcome(state, null)

        val youAreWaiting = upcoming.filter { state.waitlistPosition(state.currentMemberId, it.id) != null }
        val preferCancel = youAreWaiting.isNotEmpty() && random.nextInt(100) < 50

        return if (preferCancel || random.nextBoolean()) {
            val target = if (preferCancel) youAreWaiting.random(random) else upcoming.random(random)
            cancelSomeone(state, target, random)
        } else {
            bookSomeone(state, upcoming.random(random), random)
        }
    }

    private fun cancelSomeone(state: ClubState, workout: Workout, random: Random): Outcome<ActivityEvent?> {
        val candidates = state.bookings.filter {
            it.workoutId == workout.id && it.status == BookingStatus.BOOKED && it.memberId != state.currentMemberId
        }
        if (candidates.isEmpty()) return Outcome(state, null)
        val booking = candidates.random(random)
        val outcome = ClubRules.cancel(state, booking.memberId, workout.id)
        val result = outcome.result as? CancelResult.Cancelled ?: return Outcome(state, null)
        val you = state.currentMemberId
        val youWerePromoted = result.promoted?.id == you
        val youWereSkipped = !youWerePromoted &&
            state.waitlistPosition(you, workout.id) != null &&
            outcome.state.waitlistPosition(you, workout.id) == null
        return Outcome(outcome.state, ActivityEvent.OtherMemberCancelled(workout, youWerePromoted, youWereSkipped))
    }

    private fun bookSomeone(state: ClubState, workout: Workout, random: Random): Outcome<ActivityEvent?> {
        if (state.spotsLeft(workout) == 0) return Outcome(state, null)
        val candidates = state.members.filter {
            it.id != state.currentMemberId &&
                state.activeBooking(it.id, workout.id) == null &&
                state.waitlistPosition(it.id, workout.id) == null
        }
        if (candidates.isEmpty()) return Outcome(state, null)
        val outcome = ClubRules.book(state, candidates.random(random).id, workout.id)
        return if (outcome.result is BookingResult.Booked) {
            Outcome(outcome.state, ActivityEvent.OtherMemberBooked(workout))
        } else {
            Outcome(state, null)
        }
    }
}
