package com.aslanbek.fitclub.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aslanbek.fitclub.data.FitClubRepository
import com.aslanbek.fitclub.data.TrainerRatingService
import com.aslanbek.fitclub.domain.ClubRules
import com.aslanbek.fitclub.domain.ClubState
import com.aslanbek.fitclub.domain.ProgressStats
import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.model.Membership
import com.aslanbek.fitclub.model.Trainer
import com.aslanbek.fitclub.model.TrainerRating
import com.aslanbek.fitclub.ui.util.appContainer
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.system.measureTimeMillis

data class TrainerRow(
    val trainer: Trainer,
    val classesTogether: Int,
    val rating: TrainerRating?
)

data class RatingsLoad(
    val isLoading: Boolean = false,
    val ratings: Map<Int, TrainerRating> = emptyMap(),
    val tookMillis: Long? = null
)

data class ProgressUiState(
    val isLoading: Boolean = true,
    val memberName: String = "",
    val membership: Membership = Membership.BASIC,
    val usedThisWeek: Int = 0,
    val strikes: Int = 0,
    val blockedUntil: LocalDate? = null,
    val stats: ProgressStats? = null,
    val liveActivity: Boolean = true,
    val trainers: List<TrainerRow> = emptyList(),
    val ratingsLoading: Boolean = false,
    val ratingsTookMillis: Long? = null
)

class ProgressViewModel(
    private val repository: FitClubRepository,
    private val ratingService: TrainerRatingService
) : ViewModel() {

    private val ratingsLoad = MutableStateFlow(RatingsLoad())

    val uiState: StateFlow<ProgressUiState> = combine(repository.state, ratingsLoad) { club, ratings ->
        buildState(club, ratings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    init {
        loadRatings(forceRefresh = false)
    }

    fun loadRatings(forceRefresh: Boolean = true) {
        if (ratingsLoad.value.isLoading) return
        ratingsLoad.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            var ratings: Map<Int, TrainerRating> = emptyMap()
            val took = measureTimeMillis {
                ratings = repository.state.value.trainers
                    .map { trainer -> async { trainer.id to ratingService.fetchRating(trainer.id, forceRefresh) } }
                    .awaitAll()
                    .toMap()
            }
            ratingsLoad.value = RatingsLoad(isLoading = false, ratings = ratings, tookMillis = took)
        }
    }

    fun onMembershipChange(membership: Membership) = repository.setMembership(membership)

    fun onGoalChange(goal: Int) = repository.setWeeklyGoal(goal)

    fun onLiveActivityChange(enabled: Boolean) = repository.setLiveActivity(enabled)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ProgressViewModel(container.repository, container.ratingService)
            }
        }

        fun buildState(club: ClubState, ratings: RatingsLoad): ProgressUiState {
            val member = club.currentMember
            val strikes = club.strikesOf(member.id)
            val blockedUntil = if (ClubRules.isBlocked(club, member.id)) {
                strikes.minOf { it.updatedAt }.plusDays(ClubRules.STRIKE_WINDOW_DAYS).toLocalDate()
            } else {
                null
            }

            val attendedByTrainer = club.bookingsOf(member.id)
                .filter { it.status == BookingStatus.ATTENDED }
                .mapNotNull { club.workout(it.workoutId)?.trainerId }
                .groupingBy { it }
                .eachCount()

            val trainers = club.trainers
                .map { TrainerRow(it, attendedByTrainer[it.id] ?: 0, ratings.ratings[it.id]) }
                .sortedWith(compareByDescending<TrainerRow> { it.classesTogether }.thenBy { it.trainer.name })

            return ProgressUiState(
                isLoading = false,
                memberName = member.name,
                membership = member.membership,
                usedThisWeek = club.classesInWeek(member.id, club.today),
                strikes = strikes.size,
                blockedUntil = blockedUntil,
                stats = ProgressStats.from(club),
                liveActivity = club.liveActivity,
                trainers = trainers,
                ratingsLoading = ratings.isLoading,
                ratingsTookMillis = ratings.tookMillis
            )
        }
    }
}
