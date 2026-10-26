package com.aslanbek.fitclub.ui.schedule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aslanbek.fitclub.data.FitClubRepository
import com.aslanbek.fitclub.data.SampleData
import com.aslanbek.fitclub.domain.ClubRules
import com.aslanbek.fitclub.domain.ClubState
import com.aslanbek.fitclub.domain.WorkoutSummary
import com.aslanbek.fitclub.model.WorkoutType
import com.aslanbek.fitclub.ui.components.DayOption
import com.aslanbek.fitclub.ui.util.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val days: List<DayOption> = emptyList(),
    val selectedDay: LocalDate = LocalDate.now(),
    val query: String = "",
    val typeFilter: WorkoutType? = null,
    val onlyAvailable: Boolean = false,
    val workouts: List<WorkoutSummary> = emptyList(),
    val membershipText: String = "",
    val isBlocked: Boolean = false
)

class ScheduleViewModel(
    repository: FitClubRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dayOffset = savedStateHandle.getStateFlow(KEY_DAY_OFFSET, 0)
    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val typeFilter = savedStateHandle.getStateFlow<WorkoutType?>(KEY_TYPE, null)
    private val onlyAvailable = savedStateHandle.getStateFlow(KEY_ONLY_AVAILABLE, false)

    val uiState: StateFlow<ScheduleUiState> =
        combine(repository.state, dayOffset, query, typeFilter, onlyAvailable) { club, offset, text, type, available ->
            buildState(club, offset, text, type, available)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScheduleUiState()
        )

    fun onDaySelected(date: LocalDate) {
        savedStateHandle[KEY_DAY_OFFSET] = ChronoUnit.DAYS.between(uiState.value.today, date).toInt()
    }

    fun onQueryChange(text: String) {
        savedStateHandle[KEY_QUERY] = text
    }

    fun onTypeSelected(type: WorkoutType?) {
        savedStateHandle[KEY_TYPE] = type
    }

    fun onOnlyAvailableChange(value: Boolean) {
        savedStateHandle[KEY_ONLY_AVAILABLE] = value
    }

    companion object {
        private const val KEY_DAY_OFFSET = "dayOffset"
        private const val KEY_QUERY = "query"
        private const val KEY_TYPE = "type"
        private const val KEY_ONLY_AVAILABLE = "onlyAvailable"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ScheduleViewModel(appContainer().repository, createSavedStateHandle())
            }
        }

        fun buildState(
            club: ClubState,
            dayOffset: Int,
            query: String,
            type: WorkoutType?,
            onlyAvailable: Boolean
        ): ScheduleUiState {
            val today = club.today
            val days = SampleData.upcomingDays(today)
            val selectedDay = days.getOrElse(dayOffset) { today }
            val notEnded = club.workouts.filter { it.end > club.now }
            val countsByDay = notEnded.groupingBy { it.start.toLocalDate() }.eachCount()
            val search = query.trim()

            val visible = notEnded
                .asSequence()
                .filter { it.start.toLocalDate() == selectedDay }
                .filter { type == null || it.type == type }
                .map { club.summaryOf(it) }
                .filter { !onlyAvailable || (!it.isFull && !it.hasStarted) }
                .filter { summary ->
                    search.isEmpty() ||
                        summary.workout.title.contains(search, ignoreCase = true) ||
                        summary.trainerName.contains(search, ignoreCase = true) ||
                        summary.workout.room.contains(search, ignoreCase = true)
                }
                .sortedBy { it.workout.start }
                .toList()

            val member = club.currentMember
            val used = club.classesInWeek(member.id, today)
            val membershipText = member.membership.weeklyLimit
                ?.let { limit -> "${member.membership.label}: $used of $limit classes this week" }
                ?: "${member.membership.label}: unlimited classes · $used this week"

            return ScheduleUiState(
                isLoading = false,
                today = today,
                days = days.map { DayOption(it, countsByDay[it] ?: 0) },
                selectedDay = selectedDay,
                query = query,
                typeFilter = type,
                onlyAvailable = onlyAvailable,
                workouts = visible,
                membershipText = membershipText,
                isBlocked = ClubRules.isBlocked(club, member.id)
            )
        }
    }
}
