package com.aslanbek.fitclub.data

import com.aslanbek.fitclub.domain.ClubActivity
import com.aslanbek.fitclub.domain.ClubRules
import com.aslanbek.fitclub.domain.ClubState
import com.aslanbek.fitclub.domain.Outcome
import com.aslanbek.fitclub.domain.ActivityEvent
import com.aslanbek.fitclub.model.BookingResult
import com.aslanbek.fitclub.model.CancelResult
import com.aslanbek.fitclub.model.CheckInResult
import com.aslanbek.fitclub.model.Membership
import com.aslanbek.fitclub.model.Workout
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

interface FitClubRepository {
    val state: StateFlow<ClubState>
    val notifications: SharedFlow<String>

    fun book(workoutId: Int): BookingResult
    fun cancel(workoutId: Int): CancelResult
    fun leaveWaitlist(workoutId: Int): Boolean
    fun checkIn(workoutId: Int): CheckInResult
    fun addWorkout(workout: Workout): Workout
    fun setMembership(membership: Membership)
    fun setWeeklyGoal(goal: Int)
    fun setLiveActivity(enabled: Boolean)
    fun advanceTime(now: LocalDateTime)
    fun simulateOtherMembers(random: Random)
}

class InMemoryFitClubRepository(initial: ClubState) : FitClubRepository {
    private val lock = Any()
    private val _state = MutableStateFlow(initial)
    private val _notifications = MutableSharedFlow<String>(extraBufferCapacity = 16)

    override val state: StateFlow<ClubState> = _state.asStateFlow()
    override val notifications: SharedFlow<String> = _notifications.asSharedFlow()

    private val you: Int get() = _state.value.currentMemberId

    private fun <T> mutate(change: (ClubState) -> Outcome<T>): T = synchronized(lock) {
        val outcome = change(_state.value)
        _state.value = outcome.state
        outcome.result
    }

    override fun book(workoutId: Int): BookingResult = mutate { ClubRules.book(it, you, workoutId) }

    override fun cancel(workoutId: Int): CancelResult = mutate { ClubRules.cancel(it, you, workoutId) }

    override fun leaveWaitlist(workoutId: Int): Boolean = mutate { ClubRules.leaveWaitlist(it, you, workoutId) }

    override fun checkIn(workoutId: Int): CheckInResult = mutate { ClubRules.checkIn(it, you, workoutId) }

    override fun addWorkout(workout: Workout): Workout = mutate { ClubRules.addWorkout(it, workout) }

    override fun setMembership(membership: Membership) = mutate { state ->
        val members = state.members.map { if (it.id == state.currentMemberId) it.copy(membership = membership) else it }
        Outcome(state.copy(members = members), Unit)
    }

    override fun setWeeklyGoal(goal: Int) = mutate { Outcome(it.copy(weeklyGoal = goal.coerceIn(1, 7)), Unit) }

    override fun setLiveActivity(enabled: Boolean) = mutate { Outcome(it.copy(liveActivity = enabled), Unit) }

    override fun advanceTime(now: LocalDateTime) {
        val report = mutate { ClubRules.advanceTime(it, now) }
        report.newNoShows.forEach { workout ->
            _notifications.tryEmit("You missed \"${workout.title}\". A no-show counts as a strike.")
        }
    }

    override fun simulateOtherMembers(random: Random) {
        val event = mutate { ClubActivity.simulateOtherMember(it, random) }
        if (event !is ActivityEvent.OtherMemberCancelled) return
        val time = event.workout.start.format(NOTIFICATION_TIME)
        when {
            event.youWerePromoted ->
                _notifications.tryEmit("A spot opened in \"${event.workout.title}\" ($time). You're booked!")
            event.youWereSkipped ->
                _notifications.tryEmit("A spot opened in \"${event.workout.title}\" ($time), but your weekly limit or another class blocked it. You left the waitlist.")
        }
    }

    private companion object {
        val NOTIFICATION_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.ENGLISH)
    }
}
