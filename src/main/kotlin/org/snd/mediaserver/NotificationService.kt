package org.snd.mediaserver

import org.snd.discord.DiscordWebhookService
import org.snd.discord.model.Book
import org.snd.discord.model.Library
import org.snd.discord.model.Series
import org.snd.discord.model.WebhookMessage
import org.snd.mediaserver.model.MediaServerBookId
import org.snd.mediaserver.model.MediaServerSeriesId
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
        val books = bookIds.map { mediaServerClient.getBook(it) }
        val library = mediaServerClient.getLibrary(series.libraryId)

        if (!libraryFilter.test(library.id.id)) return null
        return WebhookMessage(
            library = Library(name = library.name),
            series = Series(id = series.id.id, name = series.name, summary = series.metadata.summary),
            books = books.map { Book(id = it.id.id, name = it.name) }
        )
    }
}
