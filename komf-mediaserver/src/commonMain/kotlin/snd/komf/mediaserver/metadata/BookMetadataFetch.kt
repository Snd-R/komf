package snd.komf.mediaserver.metadata

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderErrorEvent
import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.providers.CoreProviders
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

internal class BookFetchResult<K, V : Any>(
    val values: Map<K, V?>,
    val failures: Map<K, Exception>,
)

/**
 * Fetches a value for every non-null match. Failures are retried once after the first pass;
 * keys that still fail map to null and are listed in [BookFetchResult.failures].
 */
internal suspend fun <K, M : Any, V : Any> fetchEach(
    matches: Map<K, M?>,
    fetch: suspend (K, M) -> V,
): BookFetchResult<K, V> {
    val values = LinkedHashMap<K, V?>()
    val failures = LinkedHashMap<K, Exception>()

    suspend fun attempt(key: K, match: M) {
        try {
            values[key] = fetch(key, match)
            failures.remove(key)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            values[key] = null
            failures[key] = e
        }
    }

    for ((key, match) in matches) {
        if (match == null) values[key] = null else attempt(key, match)
    }
    for (key in failures.keys.toList()) {
        attempt(key, checkNotNull(matches[key]))
    }
    return BookFetchResult(values, failures)
}

internal class BookFailures {
    private class Failure(val provider: CoreProviders, val book: MediaServerBook, val error: Exception)

    private val failures = ConcurrentLinkedQueue<Failure>()
    private val attempted = AtomicInteger()

    fun record(provider: CoreProviders, attempted: Int, failed: Map<MediaServerBook, Exception>) {
        this.attempted.addAndGet(attempted)
        failed.forEach { (book, error) -> failures.add(Failure(provider, book, error)) }
    }

    fun failedBookIds(): Set<MediaServerBookId> = failures.mapTo(mutableSetOf()) { it.book.id }

    suspend fun toErrorEvent(): ProviderErrorEvent? {
        val first = failures.peek() ?: return null
        return ProviderErrorEvent(
            provider = first.provider,
            message = "${failures.size} of ${attempted.get()} books failed. ${first.book.name}: ${describeError(first.error)}"
        )
    }
}

/** Drops books that failed in some provider and still have no metadata, so a failure never overwrites existing data. */
internal fun <V : Any> Map<MediaServerBook, V?>.withoutFailedBlanks(failed: Set<MediaServerBookId>): Map<MediaServerBook, V?> =
    filter { (book, value) -> value != null || book.id !in failed }

internal suspend fun describeError(cause: Throwable): String =
    if (cause is ResponseException) {
        "${cause::class.simpleName}: status code ${cause.response.status} ${cause.response.bodyAsText()}"
    } else {
        "${cause::class.simpleName}: ${cause.message}"
    }
