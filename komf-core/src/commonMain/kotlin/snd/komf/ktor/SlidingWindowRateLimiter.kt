package snd.komf.ktor

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Grants at most [capacity] permits in any rolling [window], at least [minSpacing] apart, and paces them
 * evenly at one per `window / capacity` after an initial burst of [burst] permits.
 *
 * When the server rejects a request anyway, [onRateLimited] pauses every caller: the first time until
 * the oldest grant leaves the window, then for a full window on each further rejection until [onSuccess].
 */
class SlidingWindowRateLimiter(
    private val capacity: Int,
    private val window: Duration,
    private val minSpacing: Duration = Duration.ZERO,
    private val burst: Int = capacity,
    private val minCooldown: Duration = 1.minutes,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
) {
    private val mutex = Mutex()
    private val grants = ArrayDeque<ComparableTimeMark>()
    private val emissionInterval = window / capacity
    private var theoreticalArrival: ComparableTimeMark? = null
    private var blockedUntil: ComparableTimeMark? = null
    private var consecutiveCooldowns = 0

    init {
        require(capacity > 0) { "Capacity must be positive" }
        require(window.isPositive()) { "Window must be positive" }
        require(burst in 1..capacity) { "Burst must be between 1 and capacity" }
    }

    suspend fun acquire() {
        while (true) {
            val wait = mutex.withLock {
                val now = timeSource.markNow()
                val wait = waitTime(now)
                if (!wait.isPositive()) {
                    grants.addLast(now)
                    theoreticalArrival = maxOf(theoreticalArrival ?: now, now) + emissionInterval
                }
                wait
            }
            if (!wait.isPositive()) return
            delay(wait)
        }
    }

    /** Pauses all callers after the server rejected a request and returns how long the pause lasts. */
    suspend fun onRateLimited(): Duration = mutex.withLock {
        val now = timeSource.markNow()
        blockedUntil?.let { if (it > now) return@withLock it - now }

        pruneExpired(now)
        val oldest = grants.firstOrNull()
        val until = if (consecutiveCooldowns == 0 && oldest != null) {
            maxOf(now + minCooldown, oldest + window)
        } else {
            now + window
        }
        consecutiveCooldowns++
        blockedUntil = until
        until - now
    }

    /** Number of cooldowns started since the last [onSuccess]. */
    suspend fun consecutiveCooldowns(): Int = mutex.withLock { consecutiveCooldowns }

    suspend fun onSuccess() = mutex.withLock { consecutiveCooldowns = 0 }

    private fun waitTime(now: ComparableTimeMark): Duration {
        pruneExpired(now)
        val waits = buildList {
            blockedUntil?.let { add(it - now) }
            grants.lastOrNull()?.let { add(minSpacing - (now - it)) }
            theoreticalArrival?.let { add(it - emissionInterval * (burst - 1) - now) }
            if (grants.size >= capacity) add(window - (now - grants.first()))
        }
        return waits.maxOrNull() ?: Duration.ZERO
    }

    private fun pruneExpired(now: ComparableTimeMark) {
        while (grants.isNotEmpty() && now - grants.first() >= window) grants.removeFirst()
    }
}
