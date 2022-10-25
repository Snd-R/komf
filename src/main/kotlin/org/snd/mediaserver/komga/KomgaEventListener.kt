package org.snd.mediaserver.komga

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
import org.snd.mediaserver.MetadataService
import org.snd.mediaserver.NotificationService
import org.snd.mediaserver.komga.model.event.BookEvent
import org.snd.mediaserver.komga.model.event.SeriesEvent
import org.snd.mediaserver.komga.model.event.TaskQueueStatusEvent
import org.snd.mediaserver.model.MediaServer.KOMGA
import org.snd.mediaserver.model.MediaServerBookId
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.mediaserver.repository.MatchedBookRepository
import org.snd.mediaserver.repository.MatchedSeriesRepository
import java.util.function.Predicate

private val logger = KotlinLogging.logger {}

class KomgaEventListener(
    private val client: OkHttpClient,
    private val moshi: Moshi,
    private val komgaUrl: HttpUrl,

    private val metadataService: MetadataService,
    private val matchedBookRepository: MatchedBookRepository,
    private val matchedSeriesRepository: MatchedSeriesRepository,
    private val libraryFilter: Predicate<String>,
    private val notificationService: NotificationService,
) : EventSourceListener() {
    private var eventSource: EventSource? = null
    private val seriesAddedEvents: MutableList<SeriesEvent> = ArrayList()
    private val bookAddedEvents: MutableList<BookEvent> = ArrayList()
    private val seriesDeletedEvents: MutableList<SeriesEvent> = ArrayList()
    private val bookDeletedEvents: MutableList<BookEvent> = ArrayList()

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
                    bookAddedEvents.add(event)
                }
            }

            "BookDeleted" -> {
                val event = moshi.adapter<BookEvent>().fromJson(data) ?: throw RuntimeException()
                if (libraryFilter.test(event.libraryId)) {
                    bookDeletedEvents.add(event)
                }
            }

            "SeriesAdded" -> {
                val event = moshi.adapter<SeriesEvent>().fromJson(data) ?: throw RuntimeException()
                if (libraryFilter.test(event.libraryId)) {
                    seriesAddedEvents.add(event)
                }
            }

            "SeriesDeleted" -> {
                val event = moshi.adapter<SeriesEvent>().fromJson(data) ?: throw RuntimeException()
                if (libraryFilter.test(event.libraryId)) {
                    seriesDeletedEvents.add(event)
                }
            }

            "TaskQueueStatus" -> {
                val event = moshi.adapter<TaskQueueStatusEvent>().fromJson(data) ?: throw RuntimeException()
                if (event.count == 0) {
                    val events = bookAddedEvents.groupBy({ MediaServerSeriesId(it.seriesId) }, { MediaServerBookId(it.bookId) })
                    events.keys.forEach { metadataService.matchSeriesMetadata(it) }

                    bookDeletedEvents.forEach { book ->
                        matchedBookRepository.findFor(MediaServerBookId(book.bookId), KOMGA)?.let {
                            matchedBookRepository.delete(it.bookId, KOMGA)
                        }
                    }
                    seriesDeletedEvents.forEach { series ->
                        matchedSeriesRepository.findFor(MediaServerSeriesId(series.seriesId), KOMGA)?.let {
                            matchedSeriesRepository.delete(it.seriesId, KOMGA)
                        }
                    }

                    if (events.isNotEmpty()) {
                        kotlin.runCatching { notificationService.executeFor(events) }
                            .exceptionOrNull()?.let { logger.error(it) {} }
                    }

                    bookAddedEvents.clear()
                    seriesAddedEvents.clear()
                    bookDeletedEvents.clear()
                    seriesDeletedEvents.clear()
                }
            }
        }
    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        logger.error(t) { "${t?.message} ${t?.cause} response code ${response?.code}" }
        Thread.sleep(10000)
        start()
    }

    override fun onOpen(eventSource: EventSource, response: Response) {
        logger.info { "connected to komga on $komgaUrl" }
    }
}
