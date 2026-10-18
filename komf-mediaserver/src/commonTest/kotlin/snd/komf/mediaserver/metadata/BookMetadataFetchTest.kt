package snd.komf.mediaserver.metadata

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerBookMetadata
import snd.komf.mediaserver.model.MediaServerSeriesId
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

    private fun book(id: String, name: String) = MediaServerBook(
        id = MediaServerBookId(id),
        seriesId = MediaServerSeriesId("series"),
        libraryId = null,
        seriesTitle = "Series",
        name = name,
        url = "",
        number = 1,
        oneshot = false,
        metadata = MediaServerBookMetadata(
            title = name, summary = null, number = "1", numberSort = null, releaseDate = null,
            authors = emptyList(), tags = emptyList(), isbn = null, links = emptyList(),
            titleLock = false, summaryLock = false, numberLock = false, numberSortLock = false,
            releaseDateLock = false, authorsLock = false, tagsLock = false, isbnLock = false, linksLock = false,
        ),
        deleted = false,
    )

    @Test
    fun noFailuresMeansNoErrorEvent() = runTest {
        val failures = BookFailures()
        failures.record(COMIC_VINE, attempted = 3, failed = emptyMap())
        assertNull(failures.toErrorEvent())
    }

    @Test
    fun errorEventCountsFailuresAcrossProviders() = runTest {
        val failures = BookFailures()
        failures.record(COMIC_VINE, attempted = 3, failed = mapOf(book("2", "Book 2") to IllegalStateException("boom")))
        failures.record(MANGA_UPDATES, attempted = 2, failed = mapOf(book("5", "Book 5") to IllegalStateException("bang")))

        val event = failures.toErrorEvent()!!
        assertEquals(COMIC_VINE, event.provider)
        assertEquals("2 of 5 books failed. Book 2: IllegalStateException: boom", event.message)
    }

    @Test
    fun booksWithTheSameNameAreCountedSeparately() = runTest {
        val failures = BookFailures()
        failures.record(
            COMIC_VINE,
            attempted = 2,
            failed = mapOf(
                book("1", "Same") to IllegalStateException("a"),
                book("2", "Same") to IllegalStateException("b"),
            ),
        )

        assertEquals("2 of 2 books failed. Same: IllegalStateException: a", failures.toErrorEvent()!!.message)
        assertEquals(setOf(MediaServerBookId("1"), MediaServerBookId("2")), failures.failedBookIds())
    }

    @Test
    fun failedBooksWithoutMetadataAreDropped() {
        val failed = book("1", "Failed")
        val filled = book("2", "Filled")
        val failedButFilled = book("3", "FailedButFilled")
        val unmatched = book("4", "Unmatched")
        val metadata = mapOf<MediaServerBook, String?>(
            failed to null,
            filled to "meta",
            failedButFilled to "aggregated",
            unmatched to null,
        )

        val result = metadata.withoutFailedBlanks(setOf(failed.id, failedButFilled.id))

        assertEquals(setOf(filled, failedButFilled, unmatched), result.keys)
    }
}
