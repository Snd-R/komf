package snd.komf.mediaserver.metadata

import snd.komf.mediaserver.BookEvent
import snd.komf.mediaserver.MediaServerEventListener
import snd.komf.mediaserver.MetadataServiceProvider
import snd.komf.mediaserver.SeriesEvent
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.metadata.repository.BookThumbnailsRepository
import snd.komf.mediaserver.metadata.repository.SeriesMatchRepository
import snd.komf.mediaserver.metadata.repository.SeriesThumbnailsRepository
import java.util.function.Predicate

class MetadataEventHandler(
    private val metadataServiceProvider: MetadataServiceProvider,
    private val bookThumbnailsRepository: BookThumbnailsRepository,
    private val seriesThumbnailsRepository: SeriesThumbnailsRepository,
    private val seriesMatchRepository: SeriesMatchRepository,

    private val libraryFilter: Predicate<String>,
    private val seriesFilter: Predicate<String>,
) : MediaServerEventListener {

    override suspend fun onBooksAdded(events: List<BookEvent>) {
        events.filter { libraryFilter.test(it.libraryId.value) && seriesFilter.test(it.seriesId.value) }
            .groupBy { it.libraryId }
            .map { (libraryId, events) ->
                events.groupBy { MediaServerSeriesId(it.seriesId.value) }
                    .forEach { (seriesId, _) ->
                        val metadataService = metadataServiceProvider.metadataServiceFor(libraryId.value)
                        metadataService.matchSeriesMetadata(seriesId)
                    }
            }
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
