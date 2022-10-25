package org.snd.mediaserver

import org.snd.discord.DiscordWebhookService
import org.snd.discord.model.Book
import org.snd.discord.model.Library
import org.snd.discord.model.Series
import org.snd.discord.model.WebhookMessage
import org.snd.mediaserver.model.MediaServerBookId
import org.snd.mediaserver.model.MediaServerSeriesId

class NotificationService(
    private val mediaServerClient: MediaServerClient,
    private val discordWebhookService: DiscordWebhookService?,
) {

    fun executeFor(events: Map<MediaServerSeriesId, Collection<MediaServerBookId>>) {
        discordWebhookService?.let {
            events.map { (seriesId, bookIds) ->
                val series = mediaServerClient.getSeries(seriesId)
                val books = bookIds.map { mediaServerClient.getBook(it) }
                val library = mediaServerClient.getLibrary(series.libraryId)
                WebhookMessage(
                    library = Library(name = library.name),
                    series = Series(id = series.id.id, name = series.name, summary = series.metadata.summary),
                    books = books.map { Book(id = it.id.id, name = it.name) }
                )
            }.forEach { discordWebhookService.send(it) }
        }
    }
}
