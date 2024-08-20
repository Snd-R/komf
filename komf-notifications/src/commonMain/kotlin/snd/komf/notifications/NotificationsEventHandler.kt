package snd.komf.notifications

import io.github.oshai.kotlinlogging.KotlinLogging
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
import snd.komf.notifications.apprise.AppriseCliService
import snd.komf.notifications.discord.DiscordWebhookService
import snd.komf.notifications.discord.model.AlternativeTitleContext
import snd.komf.notifications.discord.model.AuthorContext
import snd.komf.notifications.discord.model.BookContext
import snd.komf.notifications.discord.model.BookMetadataContext
import snd.komf.notifications.discord.model.LibraryContext
import snd.komf.notifications.discord.model.NotificationContext
import snd.komf.notifications.discord.model.SeriesContext
import snd.komf.notifications.discord.model.SeriesMetadataContext
import snd.komf.notifications.discord.model.WebLinkContext
import java.util.function.Predicate

private val logger = KotlinLogging.logger {}

class NotificationsEventHandler(
    private val mediaServerClient: MediaServerClient,
    private val appriseService: AppriseCliService,
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
                )
            }.forEach { context ->
                runCatching { discordWebhookService.send(context) }.onFailure { logger.catching(it) }
                runCatching { appriseService.send(context) }.onFailure { logger.catching(it) }
            }
    }

    private suspend fun webhookMessage(
        seriesId: MediaServerSeriesId,
        bookIds: Collection<MediaServerBookId>,
    ): NotificationContext? {
        val series = mediaServerClient.getSeries(seriesId)
        val library = mediaServerClient.getLibrary(series.libraryId)
        if (!libraryFilter.test(library.id.value)) return null
        val books = bookIds.map { mediaServerClient.getBook(it) }

        return NotificationContext(
            library = toWebhookMessage(library),
            series = toWebhookMessage(series),
            books = books.map { toWebhookMessage(it) },
            seriesCover = mediaServerClient.getSeriesThumbnail(series.id),
            mediaServer = mediaServer.name
        )
    }

    private fun toWebhookMessage(library: MediaServerLibrary) = LibraryContext(
        id = library.id.value,
        name = library.name
    )

    private fun toWebhookMessage(series: MediaServerSeries) = SeriesContext(
        id = series.id.value,
        name = series.name,
        bookCount = series.booksCount,
        metadata = toWebhookMessage(series.metadata)
    )

    private fun toWebhookMessage(metadata: MediaServerSeriesMetadata) = SeriesMetadataContext(
        status = metadata.status.name,
        title = metadata.title,
        titleSort = metadata.titleSort,
        alternativeTitles = metadata.alternativeTitles.map { AlternativeTitleContext(it.label, it.title) },
        summary = metadata.summary,
        readingDirection = metadata.readingDirection?.name,
        publisher = metadata.publisher,
        alternativePublishers = metadata.alternativePublishers,
        ageRating = metadata.ageRating,
        language = metadata.language,
        genres = metadata.genres.toList(),
        tags = metadata.tags.toList(),
        totalBookCount = metadata.totalBookCount,
        authors = metadata.authors.map { AuthorContext(it.name, it.role) },
        releaseYear = metadata.releaseYear,
        links = metadata.links.map { WebLinkContext(it.label, it.url) },
    )


    private fun toWebhookMessage(book: MediaServerBook) = BookContext(
        id = book.id.value,
        name = book.name,
        number = book.number,
        metadata = toWebhookMessage(book.metadata)
    )

    private fun toWebhookMessage(metadata: MediaServerBookMetadata) = BookMetadataContext(
        title = metadata.title,
        summary = metadata.summary,
        number = metadata.number,
        numberSort = metadata.numberSort,
        releaseDate = metadata.releaseDate?.toString(),
        authors = metadata.authors.map { AuthorContext(it.name, it.role) },
        tags = metadata.tags.toList(),
        isbn = metadata.isbn,
        links = metadata.links.map { WebLinkContext(it.label, it.url) },
    )
}