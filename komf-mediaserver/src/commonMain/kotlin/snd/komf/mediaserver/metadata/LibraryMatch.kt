package snd.komf.mediaserver.metadata

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.takeWhile
import snd.komf.mediaserver.jobs.MetadataJobEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.CompletionEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProcessingErrorEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderErrorEvent

internal class LibraryMatchResult<T>(
    val failedFirstPass: List<T>,
    val stillFailed: List<T>,
)

/** Matches every id, then matches the ids that failed one more time. */
internal suspend fun <T> matchWithRetryPass(ids: Flow<T>, match: suspend (T) -> Boolean): LibraryMatchResult<T> {
    val failed = mutableListOf<T>()
    ids.collect { if (!match(it)) failed += it }
    val stillFailed = failed.filterNot { match(it) }
    return LibraryMatchResult(failed, stillFailed)
}

internal suspend fun Flow<MetadataJobEvent>.endedWithError(): Boolean =
    takeWhile { it !is CompletionEvent }
        .firstOrNull { it is ProviderErrorEvent || it is ProcessingErrorEvent } != null
