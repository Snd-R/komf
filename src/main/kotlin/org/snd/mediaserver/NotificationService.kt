package org.snd.mediaserver

import org.snd.mediaserver.model.mediaserver.MediaServer
import org.snd.mediaserver.model.mediaserver.MediaServerBook
import org.snd.mediaserver.model.mediaserver.MediaServerBookId
import org.snd.mediaserver.model.mediaserver.MediaServerBookMetadata
import org.snd.mediaserver.model.mediaserver.MediaServerLibrary
import org.snd.mediaserver.model.mediaserver.MediaServerSeries
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesMetadata
import org.snd.noifications.discord.DiscordWebhookService
import org.snd.noifications.discord.model.AlternativeTitleWebhookMessage
import org.snd.noifications.discord.model.AuthorWebhookMessage
import org.snd.noifications.discord.model.BookMetadataWebhookMessage
import org.snd.noifications.discord.model.BookWebhookMessage
import org.snd.noifications.discord.model.LibraryWebhookMessage
import org.snd.noifications.discord.model.SeriesMetadataWebhookMessage
import org.snd.noifications.discord.model.SeriesWebhookMessage
import org.snd.noifications.discord.model.WebLinkWebhookMessage
import org.snd.noifications.discord.model.WebhookMessage
import java.util.function.Predicate

class NotificationService(
    private val mediaServerClient: MediaServerClient,
    private val discordWebhookService: DiscordWebhookService?,
    private val libraryFilter: Predicate<String>,
) {

    fun executeFor(events: Map<MediaServerSeriesId, Collection<MediaServerBookId>>, mediaServer: MediaServer) {
        discordWebhookService?.let {
            events.mapNotNull { (seriesId, bookIds) -> webhookMessage(seriesId, bookIds, mediaServer) }
                .forEach { discordWebhookService.send(it) }
        }
    }

    private fun webhookMessage(
        seriesId: MediaServerSeriesId,
        bookIds: Collection<MediaServerBookId>,
        mediaServer: MediaServer
    ): WebhookMessage? {
        val series = mediaServerClient.getSeries(seriesId)
        val library = mediaServerClient.getLibrary(series.libraryId)
        if (!libraryFilter.test(library.id.id)) return null
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
        id = library.id.id,
        name = library.name
    )

    private fun toWebhookMessage(series: MediaServerSeries) = SeriesWebhookMessage(
        id = series.id.id,
        name = series.name,
        summary = series.metadata.summary,
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
        id = book.id.id,
        name = book.name,
        number = book.number,
        metadata = toWebhookMessage(book.metadata)
    )

    private fun toWebhookMessage(metadata: MediaServerBookMetadata) = BookMetadataWebhookMessage(
        title = metadata.title,
        summary = metadata.summary,
        number = metadata.number,
        numberSort = metadata.numberSort,
        releaseDate = metadata.releaseDate,
        authors = metadata.authors.map { AuthorWebhookMessage(it.name, it.role) },
        tags = metadata.tags.toList(),
        isbn = metadata.isbn,
        links = metadata.links.map { WebLinkWebhookMessage(it.label, it.url) },
    )
}
