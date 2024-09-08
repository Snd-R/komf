package snd.komf.mediaserver.metadata

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withTimeoutOrNull
import snd.komf.mediaserver.BookEvent
import snd.komf.mediaserver.MediaServerEventListener
import snd.komf.mediaserver.MetadataServiceProvider
import snd.komf.mediaserver.SeriesEvent
import snd.komf.mediaserver.jobs.KomfJobTracker
import snd.komf.mediaserver.jobs.MetadataJobEvent
import snd.komf.mediaserver.metadata.repository.BookThumbnailsRepository
import snd.komf.mediaserver.metadata.repository.SeriesMatchRepository
import snd.komf.mediaserver.metadata.repository.SeriesThumbnailsRepository
import snd.komf.mediaserver.model.MediaServerSeriesId
import java.util.function.Predicate
import kotlin.time.Duration.Companion.minutes

class MetadataEventHandler(
    private val metadataServiceProvider: MetadataServiceProvider,
    private val bookThumbnailsRepository: BookThumbnailsRepository,
    private val seriesThumbnailsRepository: SeriesThumbnailsRepository,
    private val seriesMatchRepository: SeriesMatchRepository,
    private val jobTracker: KomfJobTracker,

    private val libraryFilter: Predicate<String>,
    private val seriesFilter: Predicate<String>,
) : MediaServerEventListener {
    private val jobScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun onBooksAdded(events: List<BookEvent>) {
        val jobIds = events.filter { libraryFilter.test(it.libraryId.value) && seriesFilter.test(it.seriesId.value) }
            .groupBy { it.libraryId }
            .flatMap { (libraryId, events) ->
                events.groupBy { MediaServerSeriesId(it.seriesId.value) }
                    .map { (seriesId, _) ->
                        val metadataService = metadataServiceProvider.metadataServiceFor(libraryId.value)
                        metadataService.matchSeriesMetadata(seriesId)
                    }
            }
        jobIds.map { id ->
            jobScope.async {
                withTimeoutOrNull(10.minutes) {
                    jobTracker.getMetadataJobEvents(id)
                        ?.takeWhile { it !is MetadataJobEvent.CompletionEvent }
                        ?.collect {}
                }
            }
        }.forEach { it.await() }
    }

    override suspend fun onBooksDeleted(events: List<BookEvent>) {
        events.forEach { bookThumbnailsRepository.delete(it.bookId) }
    }

    override suspend fun onSeriesDeleted(events: List<SeriesEvent>) {
        events.forEach {
            seriesThumbnailsRepository.delete(it.seriesId)
            seriesMatchRepository.delete(it.seriesId)
        }
    }
}
