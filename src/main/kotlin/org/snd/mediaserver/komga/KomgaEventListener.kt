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
import org.snd.mediaserver.NotificationService
import org.snd.mediaserver.komga.model.event.BookEvent
import org.snd.mediaserver.komga.model.event.SeriesEvent
import org.snd.mediaserver.komga.model.event.TaskQueueStatusEvent
import org.snd.mediaserver.model.mediaserver.MediaServerBookId
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId
import org.snd.mediaserver.repository.BookThumbnailsRepository
import org.snd.mediaserver.repository.SeriesMatchRepository
import org.snd.mediaserver.repository.SeriesThumbnailsRepository
import org.snd.module.MediaServerModule.MetadataServiceProvider
import java.util.concurrent.ExecutorService
import java.util.function.Predicate

private val logger = KotlinLogging.logger {}

class KomgaEventListener(
    private val client: OkHttpClient,
    private val moshi: Moshi,
    private val komgaUrl: HttpUrl,

    private val metadataServiceProvider: MetadataServiceProvider,
    private val bookThumbnailsRepository: BookThumbnailsRepository,
    private val seriesThumbnailsRepository: SeriesThumbnailsRepository,
    private val seriesMatchRepository: SeriesMatchRepository,
    private val libraryFilter: Predicate<String>,
    private val notificationService: NotificationService,
    private val executor: ExecutorService,
) : EventSourceListener() {
    private var eventSource: EventSource? = null
    private val seriesAddedEvents: MutableList<SeriesEvent> = ArrayList()
    private val bookAddedEvents: MutableList<BookEvent> = ArrayList()
    private val seriesDeletedEvents: MutableList<SeriesEvent> = ArrayList()
    private val bookDeletedEvents: MutableList<BookEvent> = ArrayList()

    @Volatile
    private var isActive: Boolean = false

    fun start() {
        isActive = true
        val request = Request.Builder()
            .url(komgaUrl.newBuilder().addPathSegments("sse/v1/events").build())
            .build()
        this.eventSource = EventSources.createFactory(client).newEventSource(request, this)
    }

    @Synchronized
    fun stop() {
        isActive = false
        eventSource?.cancel()
    }

    override fun onClosed(eventSource: EventSource) {
        logger.debug { "event source closed $eventSource" }
        if (isActive) {
            reconnect()
        }
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

                    bookDeletedEvents.forEach { book ->
                        bookThumbnailsRepository.delete(MediaServerBookId(book.bookId))
                    }
                    seriesDeletedEvents.forEach { series ->
                        seriesThumbnailsRepository.delete(MediaServerSeriesId(series.seriesId))
                        seriesMatchRepository.delete(MediaServerSeriesId(series.seriesId))
                    }

                    bookAddedEvents.groupBy { it.libraryId }
                        .forEach { (libraryId, events) ->
                            val groupedEvents = events.groupBy({ MediaServerSeriesId(it.seriesId) }, { MediaServerBookId(it.bookId) })
                            executor.execute { processEvents(libraryId, groupedEvents) }
                        }

                    bookAddedEvents.clear()
                    seriesAddedEvents.clear()
                    bookDeletedEvents.clear()
                    seriesDeletedEvents.clear()
                }
            }
        }
    }

    private fun processEvents(libraryId: String, events: Map<MediaServerSeriesId, Collection<MediaServerBookId>>) {
        val metadataService = metadataServiceProvider.serviceFor(libraryId)
        events.keys.forEach { metadataService.matchSeriesMetadata(it) }
        runCatching { notificationService.executeFor(events) }
            .exceptionOrNull()?.let { logger.error(it) {} }

    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        if (isActive) {
            logger.error(t) { "${t?.message} ${t?.cause} response code ${response?.code}" }
            Thread.sleep(10000)
            reconnect()
        }
    }

    override fun onOpen(eventSource: EventSource, response: Response) {
        logger.info { "connected to komga on $komgaUrl" }
    }

    @Synchronized
    private fun reconnect() {
        if (isActive) {
            val request = Request.Builder()
                .url(komgaUrl.newBuilder().addPathSegments("sse/v1/events").build())
                .build()
            this.eventSource = EventSources.createFactory(client).newEventSource(request, this)
        }
    }
}
