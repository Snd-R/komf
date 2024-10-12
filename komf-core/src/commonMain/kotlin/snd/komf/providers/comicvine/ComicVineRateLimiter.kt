package snd.komf.providers.comicvine

import snd.komf.ktor.intervalLimiter
import snd.komf.ktor.rateLimiter
import kotlin.time.Duration.Companion.minutes

class ComicVineRateLimiter {
    private val searchLimiter = LimiterInternal()
    private val volumeLimiter = LimiterInternal()
    private val issueLimiter = LimiterInternal()
    private val storyArcLimiter = LimiterInternal()
    private val coverLimiter = LimiterInternal()

    suspend fun searchAcquire() = searchLimiter.acquire()
    suspend fun volumeAcquire() = volumeLimiter.acquire()
    suspend fun issueAcquire() = issueLimiter.acquire()
    suspend fun storyArcAcquire() = storyArcLimiter.acquire()
    suspend fun coverAcquire() = coverLimiter.acquire()

    private class LimiterInternal {
        private val burstingLimiter = intervalLimiter(50, 60.minutes)
        private val regularLimiter = rateLimiter(150, 60.minutes)

        suspend fun acquire() {
            if (!burstingLimiter.tryAcquire()) {
                regularLimiter.acquire()
            }
        }
    }
}