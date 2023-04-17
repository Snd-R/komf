package org.snd.module

import mu.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.snd.common.http.BasicAuthInterceptor
import org.snd.common.http.HttpClient
import org.snd.common.http.KavitaBearerAuthInterceptor
import org.snd.common.http.SimpleCookieJar
import org.snd.config.KavitaConfig
import org.snd.config.KomgaConfig
import org.snd.config.MetadataProcessingConfig
import org.snd.config.MetadataUpdateConfig
import org.snd.mediaserver.MediaServerClient
import org.snd.mediaserver.MetadataMerger
import org.snd.mediaserver.MetadataPostProcessor
import org.snd.mediaserver.MetadataService
import org.snd.mediaserver.MetadataUpdateMapper
import org.snd.mediaserver.MetadataUpdateService
import org.snd.mediaserver.NotificationService
import org.snd.mediaserver.kavita.KavitaAuthClient
import org.snd.mediaserver.kavita.KavitaClient
import org.snd.mediaserver.kavita.KavitaEventListener
import org.snd.mediaserver.kavita.KavitaMediaServerClientAdapter
import org.snd.mediaserver.kavita.KavitaTokenProvider
import org.snd.mediaserver.komga.KomgaClient
import org.snd.mediaserver.komga.KomgaEventListener
import org.snd.mediaserver.komga.KomgaMediaServerClientAdapter
import org.snd.mediaserver.repository.BookThumbnailsRepository
import org.snd.mediaserver.repository.SeriesMatchRepository
import org.snd.mediaserver.repository.SeriesThumbnailsRepository
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

class MediaServerModule(
    komgaConfig: KomgaConfig,
    kavitaConfig: KavitaConfig,
    okHttpClient: OkHttpClient,
    jsonModule: JsonModule,
    repositoryModule: RepositoryModule,
    private val metadataModule: MetadataModule,
    notificationsModule: NotificationsModule?,
    clock: Clock,
    systemDefaultClock: Clock
) : AutoCloseable {
    private val metadataAggregationExecutor = Executors.newFixedThreadPool(
        4,
        BasicThreadFactory.Builder().namingPattern("komf-meta-service-%d").build()
    )
    private val eventListenerExecutor = Executors.newSingleThreadExecutor(
        BasicThreadFactory.Builder().namingPattern("komf-event-listener-%d").build()
    )

    private val httpClient = okHttpClient.newBuilder()
        .cookieJar(SimpleCookieJar())
        .build()

    private val updateMapper = MetadataUpdateMapper()

    private val komgaHttpClient = httpClient
        .newBuilder()
        .readTimeout(0, SECONDS)
        .addInterceptor(BasicAuthInterceptor(komgaConfig.komgaUser, komgaConfig.komgaPassword))
        .addInterceptor(HttpLoggingInterceptor { message -> KotlinLogging.logger {}.debug { message } }.setLevel(BASIC))
        .build()

    private val komgaSseClient = httpClient.newBuilder()
        .readTimeout(0, SECONDS)
        .addInterceptor(BasicAuthInterceptor(komgaConfig.komgaUser, komgaConfig.komgaPassword))
        .addInterceptor(HttpLoggingInterceptor { message -> KotlinLogging.logger {}.debug { message } }.setLevel(BASIC))
        .build()

    private val komgaClient = KomgaClient(
        client = HttpClient(
            client = komgaHttpClient,
            name = "Komga"
        ),
        moshi = jsonModule.moshi,
        baseUrl = komgaConfig.baseUri.toHttpUrl()
    )

    private val kavitaAuthClient = KavitaAuthClient(
        client = HttpClient(
            client = httpClient
                .newBuilder()
                .addInterceptor(HttpLoggingInterceptor { message ->
                    KotlinLogging.logger {}.debug { message }
                }.setLevel(BASIC))
                .build(),
            name = "KavitaAuth"
        ),
        moshi = jsonModule.moshi,
        baseUrl = kavitaConfig.baseUri.toHttpUrl()
    )

    private val kavitaTokenProvider = KavitaTokenProvider(
        kavitaClient = kavitaAuthClient,
        apiKey = kavitaConfig.apiKey,
        verificationKey = null,
        clock = clock
    )

    private val kavitaHttpClient = httpClient
        .newBuilder()
        .addInterceptor(HttpLoggingInterceptor { message -> KotlinLogging.logger {}.debug { message } }.setLevel(BASIC))
        .addInterceptor(KavitaBearerAuthInterceptor(kavitaTokenProvider))
        .build()

    private val kavitaClient = KavitaClient(
        client = HttpClient(
            client = kavitaHttpClient,
            name = "Kavita"
        ),
        moshi = jsonModule.moshi,
        baseUrl = kavitaConfig.baseUri.toHttpUrl(),
        apiKey = kavitaConfig.apiKey
    )

    val komgaMediaServerClient = KomgaMediaServerClientAdapter(komgaClient)
    val kavitaMediaServerClient = KavitaMediaServerClientAdapter(kavitaClient)


    val komgaMetadataUpdateServiceProvider = createMetadataUpdateServiceProvider(
        config = komgaConfig.metadataUpdate,
        mediaServerClient = komgaMediaServerClient,
        seriesThumbnailsRepository = repositoryModule.komgaSeriesThumbnailsRepository,
        bookThumbnailsRepository = repositoryModule.komgaBookThumbnailsRepository
    )
    val kavitaMetadataUpdateServiceProvider = createMetadataUpdateServiceProvider(
        config = kavitaConfig.metadataUpdate,
        mediaServerClient = kavitaMediaServerClient,
        seriesThumbnailsRepository = repositoryModule.kavitaSeriesThumbnailsRepository,
        bookThumbnailsRepository = repositoryModule.kavitaBookThumbnailsRepository
    )

    val komgaMetadataServiceProvider: MetadataServiceProvider = createMetadataServiceProvider(
        config = komgaConfig.metadataUpdate,
        mediaServerClient = komgaMediaServerClient,
        seriesMatchRepository = repositoryModule.komgaSeriesMatchRepository,
        metadataUpdateServiceProvider = komgaMetadataUpdateServiceProvider
    )
    val kavitaMetadataServiceProvider: MetadataServiceProvider = createMetadataServiceProvider(
        config = kavitaConfig.metadataUpdate,
        mediaServerClient = kavitaMediaServerClient,
        seriesMatchRepository = repositoryModule.kavitaSeriesMatchRepository,
        metadataUpdateServiceProvider = kavitaMetadataUpdateServiceProvider
    )

    private val komgaNotificationService = NotificationService(
        mediaServerClient = komgaMediaServerClient,
        discordWebhookService = notificationsModule?.discordWebhookService,
        libraryFilter = {
            if (komgaConfig.notifications.libraries.isEmpty()) true
            else komgaConfig.notifications.libraries.contains(it)
        },
    )

    private val komgaEventListener = KomgaEventListener(
        client = komgaSseClient,
        moshi = jsonModule.moshi,
        komgaUrl = komgaConfig.baseUri.toHttpUrl(),
        metadataServiceProvider = komgaMetadataServiceProvider,
        libraryFilter = {
            if (komgaConfig.eventListener.libraries.isEmpty()) true
            else komgaConfig.eventListener.libraries.contains(it)
        },
        notificationService = komgaNotificationService,
        bookThumbnailsRepository = repositoryModule.komgaBookThumbnailsRepository,
        seriesThumbnailsRepository = repositoryModule.komgaSeriesThumbnailsRepository,
        seriesMatchRepository = repositoryModule.komgaSeriesMatchRepository,
        executor = eventListenerExecutor,
    )

    private val kavitaNotificationService = NotificationService(
        mediaServerClient = kavitaMediaServerClient,
        discordWebhookService = notificationsModule?.discordWebhookService,
        libraryFilter = {
            if (kavitaConfig.notifications.libraries.isEmpty()) true
            else kavitaConfig.notifications.libraries.contains(it)
        },
    )

    private val kavitaEventListener = KavitaEventListener(
        baseUrl = kavitaConfig.baseUri,
        metadataServiceProvider = kavitaMetadataServiceProvider,
        kavitaClient = kavitaClient,
        tokenProvider = kavitaTokenProvider,
        libraryFilter = {
            if (kavitaConfig.eventListener.libraries.isEmpty()) true
            else kavitaConfig.eventListener.libraries.contains(it)
        },
        notificationService = kavitaNotificationService,
        seriesMatchRepository = repositoryModule.kavitaSeriesMatchRepository,
        executor = eventListenerExecutor,
        clock = systemDefaultClock
    )

    private fun createMetadataUpdateServiceProvider(
        config: MetadataUpdateConfig,
        mediaServerClient: MediaServerClient,
        seriesThumbnailsRepository: SeriesThumbnailsRepository,
        bookThumbnailsRepository: BookThumbnailsRepository,
    ): MetadataUpdateServiceProvider {
        val defaultService = createMetadataUpdateService(
            config = config.default,
            mediaServerClient = mediaServerClient,
            seriesThumbnailsRepository = seriesThumbnailsRepository,
            bookThumbnailsRepository = bookThumbnailsRepository
        )

        val libraryServices = config.library
            .map { (libraryId, config) ->
                libraryId to createMetadataUpdateService(
                    config = config,
                    mediaServerClient = mediaServerClient,
                    seriesThumbnailsRepository = seriesThumbnailsRepository,
                    bookThumbnailsRepository = bookThumbnailsRepository
                )
            }
            .toMap()

        return MetadataUpdateServiceProvider(defaultService, libraryServices)
    }

    private fun createMetadataServiceProvider(
        config: MetadataUpdateConfig,
        mediaServerClient: MediaServerClient,
        seriesMatchRepository: SeriesMatchRepository,
        metadataUpdateServiceProvider: MetadataUpdateServiceProvider,
    ): MetadataServiceProvider {
        val defaultService = createMetadataService(
            config = config.default,
            mediaServerClient = mediaServerClient,
            seriesMatchRepository = seriesMatchRepository,
            metadataUpdateService = metadataUpdateServiceProvider.default()
        )
        val libraryServices = config.library
            .map { (libraryId, config) ->
                libraryId to createMetadataService(
                    config = config,
                    mediaServerClient = mediaServerClient,
                    seriesMatchRepository = seriesMatchRepository,
                    metadataUpdateService = metadataUpdateServiceProvider.serviceFor(libraryId)
                )
            }
            .toMap()

        return MetadataServiceProvider(defaultService, libraryServices)
    }

    private fun createMetadataService(
        config: MetadataProcessingConfig,
        metadataUpdateService: MetadataUpdateService,
        mediaServerClient: MediaServerClient,
        seriesMatchRepository: SeriesMatchRepository,
    ): MetadataService {
        return MetadataService(
            mediaServerClient = mediaServerClient,
            metadataProviders = metadataModule.metadataProviders,
            aggregateMetadata = config.aggregate,
            executor = metadataAggregationExecutor,
            metadataUpdateService = metadataUpdateService,
            seriesMatchRepository = seriesMatchRepository,
            metadataMerger = MetadataMerger(mergeTags = config.mergeTags, mergeGenres = config.mergeGenres)
        )
    }

    private fun createMetadataUpdateService(
        config: MetadataProcessingConfig,
        mediaServerClient: MediaServerClient,
        seriesThumbnailsRepository: SeriesThumbnailsRepository,
        bookThumbnailsRepository: BookThumbnailsRepository,
    ): MetadataUpdateService {
        val postProcessor = MetadataPostProcessor(config.postProcessing)

        return MetadataUpdateService(
            mediaServerClient = mediaServerClient,
            seriesThumbnailsRepository = seriesThumbnailsRepository,
            bookThumbnailsRepository = bookThumbnailsRepository,
            metadataUpdateMapper = updateMapper,
            postProcessor = postProcessor,
            comicInfoWriter = metadataModule.comicInfoWriter,
            epubWriter = metadataModule.epubWriter,

            updateModes = config.updateModes,
            uploadBookCovers = config.bookCovers,
            uploadSeriesCovers = config.seriesCovers,
        )
    }

    init {
        if (komgaConfig.eventListener.enabled)
            komgaEventListener.start()
        if (kavitaConfig.eventListener.enabled)
            kavitaEventListener.start()
    }

    override fun close() {
        komgaEventListener.stop()
        kavitaEventListener.stop()
        metadataAggregationExecutor.shutdown()
        eventListenerExecutor.shutdown()
    }

    class MetadataServiceProvider(
        private val defaultService: MetadataService,
        private val libraryServices: Map<String, MetadataService>
    ) {
        fun default() = defaultService

        fun serviceFor(libraryId: String): MetadataService {
            return libraryServices[libraryId] ?: defaultService
        }
    }

    class MetadataUpdateServiceProvider(
        private val defaultService: MetadataUpdateService,
        private val libraryServices: Map<String, MetadataUpdateService>
    ) {
        fun default() = defaultService

        fun serviceFor(libraryId: String): MetadataUpdateService {
            return libraryServices[libraryId] ?: defaultService
        }
    }
}
