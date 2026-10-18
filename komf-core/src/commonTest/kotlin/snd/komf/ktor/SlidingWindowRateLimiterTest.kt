package snd.komf.ktor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SlidingWindowRateLimiterTest {

    private fun TestScope.limiter(capacity: Int, window: Duration, minSpacing: Duration = Duration.ZERO) =
        SlidingWindowRateLimiter(
            capacity = capacity,
            window = window,
            minSpacing = minSpacing,
            timeSource = testScheduler.timeSource,
        )

    @Test
    fun grantsAtMostCapacityPerWindow() = runTest {
        val limiter = limiter(capacity = 3, window = 10.minutes)
        val times = List(4) { limiter.acquire(); currentTime }
        assertEquals(listOf(0L, 0L, 0L, 10.minutes.inWholeMilliseconds), times)
    }

    @Test
    fun neverExceedsCapacityInAnyRollingWindow() = runTest {
        val limiter = limiter(capacity = 5, window = 60.minutes)
        val times = mutableListOf<Long>()
        repeat(5) { limiter.acquire(); times += currentTime }
        advanceTimeBy(59.minutes)
        repeat(20) { limiter.acquire(); times += currentTime }

        times.windowed(6).forEach {
            assertTrue(it.last() - it.first() >= 60.minutes.inWholeMilliseconds, "6 grants within one hour: $it")
        }
    }

    @Test
    fun spacesGrants() = runTest {
        val limiter = limiter(capacity = 100, window = 60.minutes, minSpacing = 1.seconds)
        val times = List(3) { limiter.acquire(); currentTime }
        assertEquals(listOf(0L, 1000L, 2000L), times)
    }

    @Test
    fun firstCooldownWaitsForOldestGrantToLeaveWindow() = runTest {
        val limiter = limiter(capacity = 100, window = 60.minutes)
        limiter.acquire()
        advanceTimeBy(10.minutes)
        limiter.acquire()

        assertEquals(50.minutes, limiter.onRateLimited())
        limiter.acquire()
        assertEquals(60.minutes.inWholeMilliseconds, currentTime)
    }

    @Test
    fun cooldownWithoutRecentGrantsWaitsFullWindow() = runTest {
        val limiter = limiter(capacity = 100, window = 60.minutes)
        assertEquals(60.minutes, limiter.onRateLimited())
    }

    @Test
    fun cooldownIsAtLeastMinimum() = runTest {
        val limiter = limiter(capacity = 100, window = 60.minutes)
        limiter.acquire()
        advanceTimeBy(59.minutes + 30.seconds)
        assertEquals(1.minutes, limiter.onRateLimited())
    }

    @Test
    fun repeatedCooldownsWaitFullWindowUntilSuccess() = runTest {
        val limiter = limiter(capacity = 100, window = 60.minutes)
        limiter.acquire()
        advanceTimeBy(30.minutes)
        assertEquals(30.minutes, limiter.onRateLimited())
        limiter.acquire()

        assertEquals(60.minutes, limiter.onRateLimited())
        limiter.acquire()
        limiter.onSuccess()

        advanceTimeBy(30.minutes)
        assertEquals(30.minutes, limiter.onRateLimited())
    }

    @Test
    fun rateLimitDuringCooldownSharesTheDeadline() = runTest {
        val limiter = limiter(capacity = 100, window = 60.minutes)
        limiter.acquire()
        advanceTimeBy(10.minutes)

        assertEquals(50.minutes, limiter.onRateLimited())
        advanceTimeBy(5.minutes)
        assertEquals(45.minutes, limiter.onRateLimited())

        limiter.acquire()
        assertEquals(60.minutes, limiter.onRateLimited())
    }
}
