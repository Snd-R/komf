package snd.komf.notifications.discord

import snd.komf.mediaserver.BookEvent
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.MediaServerEventListener
import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerBookMetadata
import snd.komf.mediaserver.model.MediaServerLibrary
import snd.komf.mediaserver.model.MediaServerSeries
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerSeriesMetadata
import snd.komf.notifications.discord.model.AlternativeTitleWebhookMessage
import snd.komf.notifications.discord.model.AuthorWebhookMessage
import snd.komf.notifications.discord.model.BookMetadataWebhookMessage
import snd.komf.notifications.discord.model.BookWebhookMessage
import snd.komf.notifications.discord.model.LibraryWebhookMessage
import snd.komf.notifications.discord.model.SeriesMetadataWebhookMessage
import snd.komf.notifications.discord.model.SeriesWebhookMessage
import snd.komf.notifications.discord.model.WebLinkWebhookMessage
import snd.komf.notifications.discord.model.WebhookMessage
import java.util.function.Predicate

class NotificationsEventHandler(
    private val mediaServerClient: MediaServerClient,
    private val discordWebhookService: DiscordWebhookService,
    private val libraryFilter: Predicate<String>,
    private val mediaServer: MediaServer
) : MediaServerEventListener {

    override suspend fun onBooksAdded(events: List<BookEvent>) {
        events.filter { libraryFilter.test(it.libraryId.value) }
            .groupBy { it.seriesId }
            .mapNotNull { (seriesId, events) ->
                webhookMessage(
                    seriesId = seriesId,
                    bookIds = events.map { it.bookId },
                    mediaServer = mediaServer
                )
            }.forEach { discordWebhookService.send(it) }

    }

    private suspend fun webhookMessage(
        seriesId: MediaServerSeriesId,
        bookIds: Collection<MediaServerBookId>,
        mediaServer: MediaServer
    ): WebhookMessage? {
        val series = mediaServerClient.getSeries(seriesId)
        val library = mediaServerClient.getLibrary(series.libraryId)
        if (!libraryFilter.test(library.id.value)) return null
        val books = bookIds.map { mediaServerClient.getBook(it) }

        return WebhookMessage(
            library = toWebhookMessage(library),
            series = toWebhookMessage(series),
            books = books.map { toWebhookMessage(it) },
            seriesCover = mediaServerClient.getSeriesThumbnail(series.id),
            mediaServer = mediaServer.name
        )
    }

    private fun toWebhookMessage(library: MediaServerLibrary) = LibraryWebhookMessage(
        id = library.id.value,
        name = library.name
    )

    private fun toWebhookMessage(series: MediaServerSeries) = SeriesWebhookMessage(
        id = series.id.value,
        name = series.name,
        bookCount = series.booksCount,
        metadata = toWebhookMessage(series.metadata)
    )

    private fun toWebhookMessage(metadata: MediaServerSeriesMetadata) = SeriesMetadataWebhookMessage(
        status = metadata.status.name,
        title = metadata.title,
        titleSort = metadata.titleSort,
        alternativeTitles = metadata.alternativeTitles.map { AlternativeTitleWebhookMessage(it.label, it.title) },
        summary = metadata.summary,
        readingDirection = metadata.readingDirection?.name,
        publisher = metadata.publisher,
        alternativePublishers = metadata.alternativePublishers,
        ageRating = metadata.ageRating,
        language = metadata.language,
        genres = metadata.genres.toList(),
        tags = metadata.tags.toList(),
        totalBookCount = metadata.totalBookCount,
        authors = metadata.authors.map { AuthorWebhookMessage(it.name, it.role) },
        releaseYear = metadata.releaseYear,
        links = metadata.links.map { WebLinkWebhookMessage(it.label, it.url) },
    )


    private fun toWebhookMessage(book: MediaServerBook) = BookWebhookMessage(
        id = book.id.value,
        name = book.name,
        number = book.number,
        metadata = toWebhookMessage(book.metadata)
    )

    private fun toWebhookMessage(metadata: MediaServerBookMetadata) = BookMetadataWebhookMessage(
        title = metadata.title,
        summary = metadata.summary,
        number = metadata.number,
        numberSort = metadata.numberSort,
        releaseDate = metadata.releaseDate?.toString(),
        authors = metadata.authors.map { AuthorWebhookMessage(it.name, it.role) },
        tags = metadata.tags.toList(),
        isbn = metadata.isbn,
        links = metadata.links.map { WebLinkWebhookMessage(it.label, it.url) },
    )
}