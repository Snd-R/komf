package snd.komf.providers.comicvine

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.ClientRequestException
import snd.komf.ktor.SlidingWindowRateLimiter
import snd.komf.ktor.intervalLimiter
import snd.komf.ktor.rateLimiter
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

private const val RATE_LIMITED_STATUS = 420

class ComicVineRateLimiter(
    timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
    private val maxCooldowns: Int = 3,
) {
    private val searchLimiter = apiLimiter(timeSource)
    private val volumeLimiter = apiLimiter(timeSource)
    private val issueLimiter = apiLimiter(timeSource)
    private val storyArcLimiter = apiLimiter(timeSource)
    private val coverLimiter = CoverLimiter()

    suspend fun <T> search(request: suspend () -> T): T = withLimit("search", searchLimiter, request)
    suspend fun <T> volume(request: suspend () -> T): T = withLimit("volume", volumeLimiter, request)
    suspend fun <T> issue(request: suspend () -> T): T = withLimit("issue", issueLimiter, request)
    suspend fun <T> storyArc(request: suspend () -> T): T = withLimit("story_arc", storyArcLimiter, request)
    suspend fun coverAcquire() = coverLimiter.acquire()

    private suspend fun <T> withLimit(
        resource: String,
        limiter: SlidingWindowRateLimiter,
        request: suspend () -> T,
    ): T {
        var cooldowns = 0
        while (true) {
            limiter.acquire()
            try {
                return request().also { limiter.onSuccess() }
            } catch (e: ClientRequestException) {
                if (e.response.status.value != RATE_LIMITED_STATUS || cooldowns == maxCooldowns) throw e
                cooldowns++
                val pause = limiter.onRateLimited()
                logger.warn { "ComicVine $resource rate limit hit, pausing ${pause.inWholeMinutes}m (cooldown $cooldowns of $maxCooldowns)" }
            }
        }
    }

    // ComicVine allows 200 requests per resource per rolling hour; other clients may share the API key
    private fun apiLimiter(timeSource: TimeSource.WithComparableMarks) = SlidingWindowRateLimiter(
        capacity = 190,
        window = 60.minutes,
        minSpacing = 1.seconds,
        timeSource = timeSource,
    )

    private class CoverLimiter {
        private val burstingLimiter = intervalLimiter(50, 60.minutes)
        private val regularLimiter = rateLimiter(144, 60.minutes)

        suspend fun acquire() {
            if (!burstingLimiter.tryAcquire()) {
                regularLimiter.acquire()
            }
        }
    }
}
