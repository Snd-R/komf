package org.snd.komga

import org.snd.discord.DiscordWebhookService
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
                    libraryName = library.name,
                    seriesName = series.name,
                    seriesSummary = series.metadata.summary,
                    books = books.map { it.metadata.title }
                )
            }.forEach { discordWebhookService.send(it) }
        }
    }
}
