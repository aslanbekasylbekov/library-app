package com.aslanbek.fitclub.di

import com.aslanbek.fitclub.data.ClubClock
import com.aslanbek.fitclub.data.FitClubRepository
import com.aslanbek.fitclub.data.InMemoryFitClubRepository
import com.aslanbek.fitclub.data.SampleData
import com.aslanbek.fitclub.data.TrainerRatingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.LocalDateTime

class AppContainer {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository: FitClubRepository = InMemoryFitClubRepository(SampleData.create(LocalDateTime.now()))
    val ratingService = TrainerRatingService()
    val clock = ClubClock(repository, applicationScope)
}
