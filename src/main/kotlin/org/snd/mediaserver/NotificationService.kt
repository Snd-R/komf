package org.snd.mediaserver

import org.snd.mediaserver.model.MediaServerBookId
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.noifications.discord.DiscordWebhookService
import org.snd.noifications.discord.model.Book
import org.snd.noifications.discord.model.Library
import org.snd.noifications.discord.model.Series
import org.snd.noifications.discord.model.WebhookMessage
import java.util.function.Predicate

class NotificationService(
    private val mediaServerClient: MediaServerClient,
    private val discordWebhookService: DiscordWebhookService?,
    private val libraryFilter: Predicate<String>,
) {

    fun executeFor(events: Map<MediaServerSeriesId, Collection<MediaServerBookId>>) {
        discordWebhookService?.let {
            events.mapNotNull { (seriesId, bookIds) -> webhookMessage(seriesId, bookIds) }
                .forEach { discordWebhookService.send(it) }
        }
    }

    private fun webhookMessage(seriesId: MediaServerSeriesId, bookIds: Collection<MediaServerBookId>): WebhookMessage? {
        val series = mediaServerClient.getSeries(seriesId)
        val library = mediaServerClient.getLibrary(series.libraryId)
        if (!libraryFilter.test(library.id.id)) return null
        val books = bookIds.map { mediaServerClient.getBook(it) }

        return WebhookMessage(
            library = Library(name = library.name),
            series = Series(id = series.id.id, name = series.name, summary = series.metadata.summary),
            books = books.map { Book(id = it.id.id, name = it.name) },
            seriesCover = mediaServerClient.getSeriesThumbnail(series.id)
        )
    }
}
