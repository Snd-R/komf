package snd.komf.mediaserver.kavita

import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import snd.komf.mediaserver.BookEvent
import snd.komf.mediaserver.MediaServerEventListener
import snd.komf.mediaserver.SeriesEvent
import snd.komf.mediaserver.kavita.model.KavitaVolumeId
import snd.komf.mediaserver.kavita.model.events.CoverUpdateEvent
import snd.komf.mediaserver.kavita.model.events.NotificationProgressEvent
import snd.komf.mediaserver.kavita.model.events.SeriesRemovedEvent
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeriesId
import java.util.concurrent.TimeUnit.SECONDS

private val logger = KotlinLogging.logger {}

class KavitaEventHandler(
    private val baseUrl: URLBuilder,
    private val kavitaClient: KavitaClient,
    private val tokenProvider: KavitaTokenProvider,
    private val clock: Clock,
    private val eventListeners: List<MediaServerEventListener>
) {
    private val eventHandlerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val lock = ReentrantLock()
    private var hubConnection: HubConnection? = null
    private val volumesChanged: MutableList<Int> = ArrayList()

    private var lastScan: Instant = clock.now()
    private var isActive: Boolean = false

    @Synchronized
    fun start() {
        isActive = true
        val url = baseUrl.appendPathSegments("hubs", "messages")
        val hubConnection: HubConnection = HubConnectionBuilder
            .create(url.buildString())
            .withAccessTokenProvider(Single.defer { Single.just(runBlocking { tokenProvider.getToken() }) })
            .build()
        hubConnection.on("NotificationProgress", ::processProgressNotification, NotificationProgressEvent::class.java)
        hubConnection.on("CoverUpdate", ::processCoverUpdate, CoverUpdateEvent::class.java)
        hubConnection.on("SeriesRemoved", ::seriesRemoved, SeriesRemovedEvent::class.java)

        hubConnection.onClosed { reconnect(hubConnection) }
        registerInvocations(hubConnection)

        Completable.defer {
            hubConnection.start()
                .delaySubscription(10, SECONDS, Schedulers.trampoline())
                .doOnError { logger.error(it) { } }
        }
            .retry().subscribeOn(Schedulers.io()).subscribe()
        logger.info { "connecting to Kavita event listener ${url.buildString()}" }
        this.hubConnection = hubConnection
    }

    private fun reconnect(hubConnection: HubConnection) {
        if (isActive) {
            Completable.defer {
                hubConnection.start().delaySubscription(10, SECONDS, Schedulers.trampoline())
                    .doOnError { logger.error(it) { "Failed to reconnect to Kavita" } }
            }
                .retry { _ -> isActive }
                .subscribeOn(Schedulers.io()).subscribe()
        }
    }

    @Synchronized
    fun stop() {
        isActive = false
        hubConnection?.close()
    }

    private fun seriesRemoved(event: SeriesRemovedEvent) {
        val seriesEvent = SeriesEvent(
            MediaServerLibraryId(event.body.libraryId.toString()),
            MediaServerSeriesId(event.body.seriesId.toString()),
        )
        eventHandlerScope.launch {
            eventListeners.forEach { it.onSeriesDeleted(listOf(seriesEvent)) }
        }
    }

    private fun processProgressNotification(notification: NotificationProgressEvent) {
        if (notification.name == "ScanProgress" && notification.eventType == "ended") {
            val now = clock.now()
            lock.withLock {
                val volumes = volumesChanged.toList()
                eventHandlerScope.launch { processEvents(volumes, lastScan) }
                volumesChanged.clear()
                lastScan = now
            }
        }
    }

    private fun processCoverUpdate(event: CoverUpdateEvent) {
        if (event.body?.get("entityType") == "volume") {
            lock.withLock { volumesChanged.add((event.body["id"] as Double).toInt()) }
        }
    }

    private suspend fun processEvents(
        volumeIds: Collection<Int>,
        lastScan: Instant
    ) {
        val volumes = volumeIds.mapNotNull {
            try {
                kavitaClient.getVolume(KavitaVolumeId(it))
            } catch (exception: KavitaResourceNotFoundException) {
                null
            }
        }

        val newVolumes = volumes.mapNotNull { volume ->
            val newChapters = volume.chapters
                .filter { it.createdUtc.toInstant(TimeZone.UTC) > lastScan }
            if (newChapters.isEmpty()) null
            else volume to newChapters
        }.toMap()

        val seriesToChaptersMap = newVolumes.keys
            .groupBy { it.seriesId }
            .mapKeys { (s, _) -> kavitaClient.getSeries(s) }
            .mapValues { (_, v) -> v.flatMap { newVolumes[it]!! } }

        val bookEvents = seriesToChaptersMap.flatMap { (series, chapters) ->
            chapters.map {
                BookEvent(
                    MediaServerLibraryId(series.libraryId.value.toString()),
                    MediaServerSeriesId(series.id.value.toString()),
                    MediaServerBookId(it.id.value.toString())
                )
            }
        }

        eventListeners.forEach { it.onBooksAdded(bookEvents) }
    }

    private fun registerInvocations(hubConnection: HubConnection) {
        // need to add all invocation targets in order to avoid errors in logs
        // register noop handlers
        hubConnection.on("BackupDatabaseProgress", { }, Object::class.java)
        hubConnection.on("BookThemeProgress", { }, Object::class.java)
        hubConnection.on("ConvertBookmarksProgress", { }, Object::class.java)
        hubConnection.on("CleanupProgress", { }, Object::class.java)
        hubConnection.on("CoverUpdateProgress", { }, Object::class.java)
        hubConnection.on("DownloadProgress", { }, Object::class.java)
        hubConnection.on("Error", { }, Object::class.java)
        hubConnection.on("FileScanProgress", { }, Object::class.java)
        hubConnection.on("Info", { }, Object::class.java)
        hubConnection.on("LibraryModified", { }, Object::class.java)
        hubConnection.on("OnlineUsers", { }, Object::class.java)
        hubConnection.on("ScanSeries", { }, Object::class.java)
        hubConnection.on("ScanProgress", { }, Object::class.java)
        hubConnection.on("SendingToDevice", { }, Object::class.java)
        hubConnection.on("SeriesAdded", { }, Object::class.java)
        hubConnection.on("SeriesAddedToCollection", { }, Object::class.java)
        hubConnection.on("SiteThemeProgress", { }, Object::class.java)
        hubConnection.on("UpdateAvailable", { }, Object::class.java)
        hubConnection.on("UserUpdate", { }, Object::class.java)
        hubConnection.on("UserProgressUpdate", { }, Object::class.java)
        hubConnection.on("WordCountAnalyzerProgress", { }, Object::class.java)
    }
}