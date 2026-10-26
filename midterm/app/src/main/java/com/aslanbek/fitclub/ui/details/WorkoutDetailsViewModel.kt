package com.aslanbek.fitclub.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aslanbek.fitclub.data.FitClubRepository
import com.aslanbek.fitclub.data.TrainerRatingService
import com.aslanbek.fitclub.domain.ClubRules
import com.aslanbek.fitclub.domain.ClubState
import com.aslanbek.fitclub.domain.WorkoutSummary
import com.aslanbek.fitclub.model.BookingResult
import com.aslanbek.fitclub.model.CancelResult
import com.aslanbek.fitclub.model.CheckInResult
import com.aslanbek.fitclub.model.Participation
import com.aslanbek.fitclub.model.Trainer
import com.aslanbek.fitclub.model.TrainerRating
import com.aslanbek.fitclub.ui.navigation.Routes
import com.aslanbek.fitclub.ui.util.Formats
import com.aslanbek.fitclub.ui.util.appContainer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class DetailsAction(val label: String) {
    BOOK("Book a spot"),
    JOIN_WAITLIST("Join waitlist"),
    CHECK_IN("Check in"),
    CANCEL("Cancel booking"),
    LEAVE_WAITLIST("Leave waitlist")
}

data class WorkoutDetailsUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val summary: WorkoutSummary? = null,
    val trainer: Trainer? = null,
    val rating: TrainerRating? = null,
    val attendees: List<String> = emptyList(),
    val primaryAction: DetailsAction? = null,
    val secondaryAction: DetailsAction? = null,
    val hint: String = "",
    val cancelIsLate: Boolean = false
)

class WorkoutDetailsViewModel(
    private val repository: FitClubRepository,
    private val ratingService: TrainerRatingService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workoutId: Int = checkNotNull(savedStateHandle[Routes.WORKOUT_ID])
    private val rating = MutableStateFlow<TrainerRating?>(null)
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val uiState: StateFlow<WorkoutDetailsUiState> = combine(repository.state, rating) { club, loadedRating ->
        buildState(club, workoutId, loadedRating)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailsUiState())

    init {
        repository.state.value.workout(workoutId)?.let { workout ->
            viewModelScope.launch {
                rating.value = ratingService.fetchRating(workout.trainerId)
            }
        }
    }

    fun onAction(action: DetailsAction) {
        val message = when (action) {
            DetailsAction.BOOK, DetailsAction.JOIN_WAITLIST -> repository.book(workoutId).toMessage()
            DetailsAction.CANCEL -> repository.cancel(workoutId).toMessage()
            DetailsAction.LEAVE_WAITLIST ->
                if (repository.leaveWaitlist(workoutId)) "You left the waitlist" else "You are not on the waitlist"
            DetailsAction.CHECK_IN -> repository.checkIn(workoutId).toMessage()
        }
        viewModelScope.launch { _messages.send(message) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                WorkoutDetailsViewModel(container.repository, container.ratingService, createSavedStateHandle())
            }
        }

        fun buildState(club: ClubState, workoutId: Int, rating: TrainerRating?): WorkoutDetailsUiState {
            val workout = club.workout(workoutId)
                ?: return WorkoutDetailsUiState(isLoading = false, notFound = true)
            val summary = club.summaryOf(workout)
            val attendees = club.bookings
                .filter { it.workoutId == workoutId && it.status.holdsSpot }
                .mapNotNull { club.member(it.memberId) }
                .map { if (it.id == club.currentMemberId) "You" else it.name }

            var primary: DetailsAction? = null
            var secondary: DetailsAction? = null
            val checkInOpensAt = ClubRules.checkInOpensAt(workout)
            val late = ClubRules.isLateCancellation(club.now, workout)
            val hint = when (val participation = summary.participation) {
                Participation.Booked -> when {
                    summary.hasEnded -> "This class has ended."
                    club.now >= checkInOpensAt -> {
                        primary = DetailsAction.CHECK_IN
                        if (!summary.hasStarted) secondary = DetailsAction.CANCEL
                        "Check-in is open. Show up and tap Check in, or it will count as a no-show."
                    }
                    else -> {
                        primary = DetailsAction.CANCEL
                        val opens = "Check-in opens at ${Formats.time(checkInOpensAt)}."
                        if (late) {
                            "Less than ${ClubRules.FREE_CANCEL_HOURS} h to start: cancelling now counts as a strike. $opens"
                        } else {
                            "Free cancellation until ${Formats.time(ClubRules.freeCancellationUntil(workout))}. $opens"
                        }
                    }
                }
                Participation.Attended -> "You attended this class. Great job: about ${workout.estimatedCalories} kcal burned."
                Participation.Missed -> "You missed this class. A no-show counts as a strike."
                is Participation.Waitlisted -> {
                    if (!summary.hasStarted) primary = DetailsAction.LEAVE_WAITLIST
                    "You are #${participation.position} on the waitlist. When a spot opens, we book you automatically."
                }
                Participation.None -> when {
                    summary.hasStarted -> "This class has already started."
                    summary.isFull -> {
                        primary = DetailsAction.JOIN_WAITLIST
                        "The class is full. ${summary.waitlistSize} people are waiting."
                    }
                    else -> {
                        primary = DetailsAction.BOOK
                        "${summary.spotsLeft} of ${workout.capacity} spots left."
                    }
                }
            }

            return WorkoutDetailsUiState(
                isLoading = false,
                today = club.today,
                summary = summary,
                trainer = club.trainer(workout.trainerId),
                rating = rating,
                attendees = attendees,
                primaryAction = primary,
                secondaryAction = secondary,
                hint = hint,
                cancelIsLate = late
            )
        }

        private fun BookingResult.toMessage(): String = when (this) {
            is BookingResult.Booked -> "You're booked for \"${workout.title}\""
            is BookingResult.Waitlisted -> "Class is full. You are #$position on the waitlist"
            is BookingResult.AlreadyBooked -> "You are already booked"
            is BookingResult.AlreadyWaitlisted -> "You are already #$position on the waitlist"
            is BookingResult.LimitReached -> "Basic plan allows $limit classes per week. Upgrade on the Progress tab"
            is BookingResult.TimeConflict -> "Overlaps with \"${other.title}\" at ${Formats.time(other.start)}"
            is BookingResult.Blocked -> "Booking paused until ${Formats.dayMonth(until)}: $strikes strikes"
            is BookingResult.AlreadyStarted -> "This class has already started"
            BookingResult.WorkoutNotFound -> "Class not found"
        }

        private fun CancelResult.toMessage(): String = when (this) {
            is CancelResult.Cancelled -> buildString {
                append(if (late) "Late cancellation: strike added" else "Booking cancelled")
                if (promoted != null) append(". ${promoted.name} got your spot from the waitlist")
            }
            is CancelResult.AlreadyStarted -> "The class has started and can't be cancelled"
            CancelResult.NotBooked -> "You are not booked for this class"
            CancelResult.WorkoutNotFound -> "Class not found"
        }

        private fun CheckInResult.toMessage(): String = when (this) {
            is CheckInResult.CheckedIn -> "Checked in. Enjoy \"${workout.title}\"!"
            is CheckInResult.TooEarly -> "Check-in opens at ${Formats.time(opensAt)}"
            CheckInResult.Finished -> "The class has already finished"
            CheckInResult.NotBooked -> "Book the class first"
            CheckInResult.WorkoutNotFound -> "Class not found"
        }
    }
}
