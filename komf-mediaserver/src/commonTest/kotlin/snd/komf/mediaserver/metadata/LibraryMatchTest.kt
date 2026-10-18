package snd.komf.mediaserver.metadata

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import snd.komf.mediaserver.jobs.MetadataJobEvent.CompletionEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProcessingErrorEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderBookEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderCompletedEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderErrorEvent
import snd.komf.providers.CoreProviders.COMIC_VINE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryMatchTest {

    @Test
    fun failedSeriesAreRetriedOnceAfterTheFullPass() = runTest {
        val calls = mutableListOf<Int>()
        val result = matchWithRetryPass(listOf(1, 2, 3, 4).asFlow()) { id ->
            calls += id
            when (id) {
                2 -> false
                3 -> calls.count { it == 3 } > 1
                else -> true
            }
        }

        assertEquals(listOf(1, 2, 3, 4, 2, 3), calls)
        assertEquals(listOf(2, 3), result.failedFirstPass)
        assertEquals(listOf(2), result.stillFailed)
    }

    @Test
    fun providerErrorMeansFailed() = runTest {
        val events = flowOf(
            ProviderBookEvent(COMIC_VINE, 2, 1),
            ProviderErrorEvent(COMIC_VINE, "1 of 2 books failed"),
            CompletionEvent,
        )
        assertTrue(events.endedWithError())
    }

    @Test
    fun processingErrorMeansFailed() = runTest {
        assertTrue(flowOf(ProcessingErrorEvent("boom"), CompletionEvent).endedWithError())
    }

    @Test
    fun completionWithoutErrorMeansSucceeded() = runTest {
        assertFalse(flowOf(ProviderCompletedEvent(COMIC_VINE), CompletionEvent).endedWithError())
    }

    @Test
    fun eventsAfterCompletionAreIgnored() = runTest {
        assertFalse(flowOf(CompletionEvent, ProcessingErrorEvent("late")).endedWithError())
    }
}
