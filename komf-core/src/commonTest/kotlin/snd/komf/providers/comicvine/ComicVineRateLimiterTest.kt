package snd.komf.providers.comicvine

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ComicVineRateLimiterTest {

    private fun responseException(status: Int): ClientRequestException = runBlocking {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode(status, "")) }) { expectSuccess = true }
        assertFailsWith<ClientRequestException> { client.get("http://comicvine.test/") }
    }

    private val rateLimited = responseException(420)
    private val notFound = responseException(404)

    @Test
    fun retriesSameRequestAfterCooldown() = runTest {
        val limiter = ComicVineRateLimiter(timeSource = testScheduler.timeSource)
        val calls = mutableListOf<Long>()

        val result = limiter.issue {
            calls += currentTime
            if (calls.size == 1) throw rateLimited
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(listOf(0L, 60.minutes.inWholeMilliseconds), calls)
    }

    @Test
    fun givesUpAfterMaxCooldowns() = runTest {
        val limiter = ComicVineRateLimiter(timeSource = testScheduler.timeSource)
        var calls = 0

        assertFailsWith<ClientRequestException> {
            limiter.issue<String> { calls++; throw rateLimited }
        }
        assertEquals(4, calls)
    }

    @Test
    fun otherErrorsAreNotRetried() = runTest {
        val limiter = ComicVineRateLimiter(timeSource = testScheduler.timeSource)
        var calls = 0

        assertFailsWith<ClientRequestException> {
            limiter.volume<String> { calls++; throw notFound }
        }
        assertEquals(1, calls)
    }

    @Test
    fun cooldownPausesSameResourceOnly() = runTest {
        val limiter = ComicVineRateLimiter(timeSource = testScheduler.timeSource)
        var first = true

        val rejected = async {
            limiter.issue {
                if (first) {
                    first = false
                    throw rateLimited
                }
                currentTime
            }
        }
        advanceTimeBy(10.seconds)
        val sameResource = async { limiter.issue { currentTime } }
        val otherResource = async { limiter.volume { currentTime } }

        assertEquals(10.seconds.inWholeMilliseconds, otherResource.await())
        assertTrue(sameResource.await() >= 60.minutes.inWholeMilliseconds)
        assertTrue(rejected.await() >= 60.minutes.inWholeMilliseconds)
    }
}
