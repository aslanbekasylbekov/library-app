package com.aslanbek.fitclub.ui.addworkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aslanbek.fitclub.data.FitClubRepository
import com.aslanbek.fitclub.data.SampleData
import com.aslanbek.fitclub.domain.ClubState
import com.aslanbek.fitclub.domain.NewWorkoutInput
import com.aslanbek.fitclub.domain.WorkoutValidation
import com.aslanbek.fitclub.domain.WorkoutValidator
import com.aslanbek.fitclub.model.Trainer
import com.aslanbek.fitclub.ui.util.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class AddWorkoutUiState(
    val today: LocalDate,
    val days: List<LocalDate>,
    val trainers: List<Trainer>
)

class AddWorkoutViewModel(private val repository: FitClubRepository) : ViewModel() {

    val uiState: StateFlow<AddWorkoutUiState> = repository.state
        .map { buildState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildState(repository.state.value))

    fun save(input: NewWorkoutInput): WorkoutValidation {
        val club = repository.state.value
        return when (val result = WorkoutValidator.validate(input, club.now, club.workouts)) {
            is WorkoutValidation.Valid -> WorkoutValidation.Valid(repository.addWorkout(result.workout))
            is WorkoutValidation.Invalid -> result
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AddWorkoutViewModel(appContainer().repository) }
        }

        private fun buildState(club: ClubState) = AddWorkoutUiState(
            today = club.today,
            days = SampleData.upcomingDays(club.today),
            trainers = club.trainers
        )
    }
}
