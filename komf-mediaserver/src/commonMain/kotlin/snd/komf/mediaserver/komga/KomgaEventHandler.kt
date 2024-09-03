package snd.komf.mediaserver.komga

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import snd.komf.mediaserver.BookEvent
import snd.komf.mediaserver.MediaServerEventListener
import snd.komf.mediaserver.SeriesEvent
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komga.client.sse.KomgaEvent
import snd.komga.client.sse.KomgaSSESession

private val logger = KotlinLogging.logger {}

class KomgaEventHandler(
    private val eventSourceFactory: suspend () -> KomgaSSESession,
    private val eventListeners: List<MediaServerEventListener>
) {
    private val eventHandlerScope = CoroutineScope(
        Dispatchers.Default +
                SupervisorJob() +
                CoroutineExceptionHandler { _, exception -> logger.catching(exception) }
    )
    private val mutex = Mutex()

    private val bookAddedEvents: MutableList<KomgaEvent.BookEvent> = mutableListOf()
    private val seriesDeletedEvents: MutableList<KomgaEvent.SeriesEvent> = mutableListOf()
    private val bookDeletedEvents: MutableList<KomgaEvent.BookEvent> = mutableListOf()
    private var eventSource: KomgaSSESession? = null


    fun start() {
        eventHandlerScope.launch {
            val eventSource = eventSourceFactory()
            this@KomgaEventHandler.eventSource = eventSource
            logger.info { "Connected to Komga event source" }

            eventSource.incoming.onEach { event ->
                logger.debug { event }
                when (event) {
                    is KomgaEvent.BookAdded -> mutex.withLock { bookAddedEvents.add(event) }
                    is KomgaEvent.BookDeleted -> mutex.withLock { bookDeletedEvents.add(event) }
                    is KomgaEvent.SeriesDeleted -> mutex.withLock { seriesDeletedEvents.add(event) }
                    is KomgaEvent.TaskQueueStatus -> {
                        if (event.count != 0) return@onEach

                        mutex.withLock {
                            processEvents(
                                bookAddedEvents,
                                seriesDeletedEvents,
                                bookDeletedEvents
                            )

                            bookAddedEvents.clear()
                            bookDeletedEvents.clear()
                            seriesDeletedEvents.clear()
                        }
                    }

                    else -> {}
                }
            }.launchIn(eventHandlerScope)
        }
    }

    private fun processEvents(
        bookAddedEvents: List<KomgaEvent.BookEvent>,
        seriesDeletedEvents: List<KomgaEvent.SeriesEvent>,
        bookDeletedEvents: List<KomgaEvent.BookEvent>,
    ) {
        eventListeners.forEach { listener ->
            if (bookAddedEvents.isNotEmpty()) {
                val bookEvents = bookAddedEvents.map { it.toMediaServerEvent() }
                eventHandlerScope.launch { listener.onBooksAdded(bookEvents) }

            }

            if (bookDeletedEvents.isNotEmpty()) {
                val bookEvents = bookDeletedEvents.map { it.toMediaServerEvent() }
                eventHandlerScope.launch { listener.onBooksDeleted(bookEvents) }
            }

            if (seriesDeletedEvents.isNotEmpty()) {
                val seriesEvents = seriesDeletedEvents.map { it.toMediaServerEvent() }
                eventHandlerScope.launch { listener.onSeriesDeleted(seriesEvents) }
            }
        }
    }

    fun stop() {
        eventSource?.cancel()
        eventHandlerScope.cancel()
    }

    private fun KomgaEvent.BookEvent.toMediaServerEvent() = BookEvent(
        libraryId = MediaServerLibraryId(libraryId.value),
        seriesId = MediaServerSeriesId(seriesId.value),
        bookId = MediaServerBookId(bookId.value)
    )

    private fun KomgaEvent.SeriesEvent.toMediaServerEvent() = SeriesEvent(
        libraryId = MediaServerLibraryId(libraryId.value),
        seriesId = MediaServerSeriesId(seriesId.value),
    )
}
