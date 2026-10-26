package com.aslanbek.fitclub.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aslanbek.fitclub.data.FitClubRepository
import com.aslanbek.fitclub.domain.ClubRules
import com.aslanbek.fitclub.domain.ClubState
import com.aslanbek.fitclub.domain.WorkoutSummary
import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.model.CheckInResult
import com.aslanbek.fitclub.ui.util.Formats
import com.aslanbek.fitclub.ui.util.appContainer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate

data class NextClass(
    val summary: WorkoutSummary,
    val startsIn: String,
    val canCheckIn: Boolean
)

data class HistoryEntry(val bookingId: Int, val summary: WorkoutSummary, val status: BookingStatus)

data class BookingsUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val next: NextClass? = null,
    val upcoming: List<WorkoutSummary> = emptyList(),
    val waitlist: List<WorkoutSummary> = emptyList(),
    val history: List<HistoryEntry> = emptyList()
)

class BookingsViewModel(private val repository: FitClubRepository) : ViewModel() {

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val uiState: StateFlow<BookingsUiState> = repository.state
        .map { buildState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookingsUiState())

    fun checkIn(workoutId: Int) {
        val message = when (val result = repository.checkIn(workoutId)) {
            is CheckInResult.CheckedIn -> "Checked in. Enjoy \"${result.workout.title}\"!"
            is CheckInResult.TooEarly -> "Check-in opens at ${Formats.time(result.opensAt)}"
            CheckInResult.Finished -> "The class has already finished"
            CheckInResult.NotBooked -> "You are not booked for this class"
            CheckInResult.WorkoutNotFound -> "Class not found"
        }
        viewModelScope.launch { _messages.send(message) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { BookingsViewModel(appContainer().repository) }
        }

        fun buildState(club: ClubState): BookingsUiState {
            val mine = club.bookingsOf(club.currentMemberId)
                .mapNotNull { booking -> club.workout(booking.workoutId)?.let { booking to it } }

            val upcoming = mine
                .filter { (booking, _) -> booking.status == BookingStatus.BOOKED }
                .map { (_, workout) -> club.summaryOf(workout) }
                .sortedBy { it.workout.start }

            val waitlist = club.waitlists
                .filterValues { club.currentMemberId in it }
                .keys
                .mapNotNull { club.workout(it) }
                .map { club.summaryOf(it) }
                .sortedBy { it.workout.start }

            val history = mine
                .filter { (booking, _) -> booking.status != BookingStatus.BOOKED }
                .sortedByDescending { (_, workout) -> workout.start }
                .map { (booking, workout) -> HistoryEntry(booking.id, club.summaryOf(workout), booking.status) }

            val next = upcoming.firstOrNull()?.let { summary ->
                NextClass(
                    summary = summary,
                    startsIn = startsInText(Duration.between(club.now, summary.workout.start)),
                    canCheckIn = club.now >= ClubRules.checkInOpensAt(summary.workout) && !summary.hasEnded
                )
            }

            return BookingsUiState(
                isLoading = false,
                today = club.today,
                next = next,
                upcoming = upcoming,
                waitlist = waitlist,
                history = history
            )
        }

        fun startsInText(duration: Duration): String {
            if (duration.isNegative || duration.isZero) return "In progress"
            val minutes = duration.toMinutes()
            return when {
                minutes < 1 -> "Starts in less than a minute"
                minutes < 60 -> "Starts in $minutes min"
                minutes < 24 * 60 -> "Starts in ${minutes / 60} h ${minutes % 60} min"
                else -> "Starts in ${minutes / (24 * 60)} d ${minutes % (24 * 60) / 60} h"
            }
        }
    }
}
