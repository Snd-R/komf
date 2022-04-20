package org.snd.komga

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import mu.KotlinLogging
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.snd.komga.model.dto.KomgaBookId
import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.komga.model.event.BookEvent
import org.snd.komga.model.event.SeriesEvent
import org.snd.komga.model.event.TaskQueueStatusEvent
import org.snd.komga.webhook.DiscordWebhooks
import java.util.function.Predicate

private val logger = KotlinLogging.logger {}

class KomgaEventListener(
    private val client: OkHttpClient,
    private val moshi: Moshi,
    private val komgaUrl: HttpUrl,
    private val komgaService: KomgaService,
    private val libraryFilter: Predicate<String>,
    private val discordWebhooks: DiscordWebhooks?,
) : EventSourceListener() {
    private var eventSource: EventSource? = null
    private val seriesEvents: MutableList<SeriesEvent> = ArrayList()
    private val bookEvents: MutableList<BookEvent> = ArrayList()

    fun start() {
        val request = Request.Builder()
            .url(komgaUrl.newBuilder().addPathSegments("sse/v1/events").build())
            .build()
        this.eventSource = EventSources.createFactory(client).newEventSource(request, this)
    }

    fun stop() {
        eventSource?.cancel()
    }

    override fun onClosed(eventSource: EventSource) {
        logger.debug { "event source closed $eventSource" }
        start()
    }

    @Synchronized
    override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
        logger.debug { "event: $type data: $data" }
        when (type) {
            "BookAdded" -> {
                val event = moshi.adapter<BookEvent>().fromJson(data) ?: throw RuntimeException()
                if (libraryFilter.test(event.libraryId)) {
                    bookEvents.add(event)
                }
            }
            "SeriesAdded" -> {
                val event = moshi.adapter<SeriesEvent>().fromJson(data) ?: throw RuntimeException()
                if (libraryFilter.test(event.libraryId)) {
                    seriesEvents.add(event)
                }
            }
            "TaskQueueStatus" -> {
                if (seriesEvents.isNotEmpty()) {
                    val event = moshi.adapter<TaskQueueStatusEvent>().fromJson(data) ?: throw RuntimeException()
                    if (event.count == 0) {
                        val events = bookEvents.groupBy({ KomgaSeriesId(it.seriesId) }, { KomgaBookId(it.bookId) })
                        events.keys.forEach { komgaService.matchSeriesMetadata(it) }
                        discordWebhooks?.executeFor(events)

                        seriesEvents.clear()
                        bookEvents.clear()
                    }
                }
            }
        }
    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        logger.error { "${t?.message} ${t?.cause} response code ${response?.code}" }
        Thread.sleep(10000)
        start()
    }

    override fun onOpen(eventSource: EventSource, response: Response) {
        logger.info { "connected to komga on $komgaUrl" }
    }
}
