package com.aslanbek.fitclub.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.random.Random

class ClubClock(
    private val repository: FitClubRepository,
    private val scope: CoroutineScope,
    private val tickMillis: Long = 15_000L,
    private val now: () -> LocalDateTime = { LocalDateTime.now() },
    private val random: Random = Random.Default
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                delay(tickMillis)
                repository.advanceTime(now())
                if (repository.state.value.liveActivity && random.nextInt(100) < 60) {
                    repository.simulateOtherMembers(random)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
