package org.snd.komga

import org.snd.discord.DiscordWebhookService
import org.snd.discord.model.Book
import org.snd.discord.model.Library
import org.snd.discord.model.Series
import org.snd.discord.model.WebhookMessage
import org.snd.komga.model.dto.KomgaBookId
import org.snd.komga.model.dto.KomgaLibraryId
import org.snd.komga.model.dto.KomgaSeriesId

class KomgaNotificationService(
    private val komgaClient: KomgaClient,
    private val discordWebhookService: DiscordWebhookService?,
) {

    fun executeFor(events: Map<KomgaSeriesId, Collection<KomgaBookId>>) {
        discordWebhookService?.let {
            events.map { (seriesId, bookIds) ->
                val series = komgaClient.getSeries(seriesId)
                val books = bookIds.map { komgaClient.getBook(it) }
                val library = komgaClient.getLibrary(KomgaLibraryId(series.libraryId))
                WebhookMessage(
                    library = Library(name = library.name),
                    series = Series(id = series.id, name = series.name, summary = series.metadata.summary),
                    books = books.map { Book(id = it.id, name = it.name) }
                )
            }.forEach { discordWebhookService.send(it) }
        }
    }
}
