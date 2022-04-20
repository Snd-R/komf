package org.snd.komga.webhook

import org.snd.komga.KomgaClient
import org.snd.komga.model.dto.KomgaBook
import org.snd.komga.model.dto.KomgaBookId
import org.snd.komga.model.dto.KomgaLibrary
import org.snd.komga.model.dto.KomgaLibraryId
import org.snd.komga.model.dto.KomgaSeries
import org.snd.komga.model.dto.KomgaSeriesId

class DiscordWebhooks(
    private val webhooks: Collection<String>,
    private val komgaClient: KomgaClient,
    private val discordClient: DiscordClient,
) {

    fun executeFor(events: Map<KomgaSeriesId, Collection<KomgaBookId>>) {
        val messages = events.map { (seriesId, bookIds) ->
            val series = komgaClient.getSeries(seriesId)
            val books = bookIds.map { komgaClient.getBook(it) }
            series to books
        }.map { (series, books) ->
            val library = komgaClient.getLibrary(KomgaLibraryId(series.libraryId))
            toRequest(library, series, books)
        }

        webhooks.map { discordClient.getWebhook(it) }
            .forEach { webhook ->
                messages.forEach { request -> discordClient.executeWebhook(webhook, request) }
            }
    }

    private fun toRequest(library: KomgaLibrary, series: KomgaSeries, books: Collection<KomgaBook>): WebhookExecuteRequest {
        val embed = Embed(
            title = "${library.name} - ${series.name}",
            description = books.sortedBy { it.name }.joinToString("\n\n") { it.name },
            color = "1F8B4C".toInt(16)
        )

        return WebhookExecuteRequest(embeds = listOf(embed))
    }

}
