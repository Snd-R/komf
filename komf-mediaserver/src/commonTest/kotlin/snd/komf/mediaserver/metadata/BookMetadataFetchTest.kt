package snd.komf.mediaserver.metadata

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import snd.komf.providers.CoreProviders.COMIC_VINE
import snd.komf.providers.CoreProviders.MANGA_UPDATES
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookMetadataFetchTest {

    @Test
    fun failedBookIsNullAndOthersAreKept() = runTest {
        val calls = mutableListOf<String>()
        val result = fetchEach(mapOf("a" to 1, "b" to 2, "c" to null)) { key, match ->
            calls += key
            if (key == "b") error("boom")
            "meta$match"
        }

        assertEquals(mapOf("a" to "meta1", "b" to null, "c" to null), result.values)
        assertEquals(setOf("b"), result.failures.keys)
        assertEquals(listOf("a", "b", "b"), calls)
    }

    @Test
    fun failedBookIsRetriedOnceAfterFirstPass() = runTest {
        val calls = mutableListOf<String>()
        val result = fetchEach(mapOf("a" to 1, "b" to 2, "c" to 3)) { key, match ->
            calls += key
            if (key == "b" && calls.count { it == "b" } == 1) error("transient")
            "meta$match"
        }

        assertEquals(mapOf("a" to "meta1", "b" to "meta2", "c" to "meta3"), result.values)
        assertTrue(result.failures.isEmpty())
        assertEquals(listOf("a", "b", "c", "b"), calls)
    }

    @Test
    fun cancellationIsNotSwallowed() = runTest {
        assertFailsWith<CancellationException> {
            fetchEach(mapOf("a" to 1)) { _, _ -> throw CancellationException("cancelled") }
        }
    }

    @Test
    fun noFailuresMeansNoErrorEvent() = runTest {
        val failures = BookFailures()
        failures.record(COMIC_VINE, attempted = 3, failed = emptyMap())
        assertNull(failures.toErrorEvent())
    }

    @Test
    fun errorEventCountsFailuresAcrossProviders() = runTest {
        val failures = BookFailures()
        failures.record(COMIC_VINE, attempted = 3, failed = mapOf("Book 2" to IllegalStateException("boom")))
        failures.record(MANGA_UPDATES, attempted = 2, failed = mapOf("Book 5" to IllegalStateException("bang")))

        val event = failures.toErrorEvent()!!
        assertEquals(COMIC_VINE, event.provider)
        assertEquals("2 of 5 books failed. Book 2: IllegalStateException: boom", event.message)
    }
}
