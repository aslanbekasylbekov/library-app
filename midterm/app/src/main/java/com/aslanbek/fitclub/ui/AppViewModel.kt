package com.aslanbek.fitclub.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aslanbek.fitclub.data.FitClubRepository
import com.aslanbek.fitclub.model.BookingStatus
import com.aslanbek.fitclub.ui.util.appContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppViewModel(repository: FitClubRepository) : ViewModel() {

    val notifications: Flow<String> = repository.notifications

    val upcomingCount: StateFlow<Int> = repository.state
        .map { club -> club.bookingsOf(club.currentMemberId).count { it.status == BookingStatus.BOOKED } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AppViewModel(appContainer().repository) }
        }
    }
}
