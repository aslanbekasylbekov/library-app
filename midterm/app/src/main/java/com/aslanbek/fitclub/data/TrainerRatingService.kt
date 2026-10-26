package com.aslanbek.fitclub.data

import com.aslanbek.fitclub.model.TrainerRating
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrainerRatingService(private val latencyMillis: LongRange = 400L..1200L) {
    private val cache = mutableMapOf<Int, TrainerRating>()
    private val mutex = Mutex()

    suspend fun fetchRating(trainerId: Int, forceRefresh: Boolean = false): TrainerRating {
        if (!forceRefresh) {
            mutex.withLock { cache[trainerId] }?.let { return it }
        }

        delay(latencyMillis.random())
        val rating = TrainerRating(
            trainerId = trainerId,
            average = 4.2 + (trainerId * 37 % 8) / 10.0,
            reviews = 40 + trainerId * 23 % 90
        )
        mutex.withLock { cache[trainerId] = rating }
        return rating
    }
}
