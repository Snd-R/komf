package org.snd.module

import mu.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.snd.config.KavitaConfig
import org.snd.config.KomgaConfig
import org.snd.infra.BasicAuthInterceptor
import org.snd.infra.HttpClient
import org.snd.infra.KavitaBearerAuthInterceptor
import org.snd.infra.SimpleCookieJar
import org.snd.mediaserver.MetadataService
import org.snd.mediaserver.MetadataUpdateMapper
import org.snd.mediaserver.MetadataUpdateService
import org.snd.mediaserver.NotificationService
import org.snd.mediaserver.kavita.*
import org.snd.mediaserver.komga.KomgaClient
import org.snd.mediaserver.komga.KomgaEventListener
import org.snd.mediaserver.komga.KomgaMediaServerClientAdapter
import org.snd.mediaserver.model.MediaServer.KAVITA
import org.snd.mediaserver.model.MediaServer.KOMGA
import java.time.Clock
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

class MediaServerModule(
    komgaConfig: KomgaConfig,
    kavitaConfig: KavitaConfig,
    okHttpClient: OkHttpClient,
    jsonModule: JsonModule,
    repositoryModule: RepositoryModule,
    metadataModule: MetadataModule,
    discordModule: DiscordModule?,
    clock: Clock,
    systemDefaultClock: Clock
) : AutoCloseable {
    private val metadataAggregationExecutor = Executors.newFixedThreadPool(
        4,
        BasicThreadFactory.Builder().namingPattern("komf-meta-aggregator-%d").build()
    )
    private val eventListenerExecutor = Executors.newSingleThreadExecutor(
        BasicThreadFactory.Builder().namingPattern("komf-event-listener-%d")
            .build()
    )

    private val httpClient = okHttpClient.newBuilder()
        .cookieJar(SimpleCookieJar())
        .build()

    private val komgaHttpClient = httpClient
        .newBuilder()
        .readTimeout(0, SECONDS)
        .addInterceptor(BasicAuthInterceptor(komgaConfig.komgaUser, komgaConfig.komgaPassword))
        .addInterceptor(HttpLoggingInterceptor { message ->
            KotlinLogging.logger {}.debug { message }
        }.setLevel(BASIC))
        .build()

    private val komgaSseClient = httpClient.newBuilder()
        .readTimeout(0, SECONDS)
        .addInterceptor(BasicAuthInterceptor(komgaConfig.komgaUser, komgaConfig.komgaPassword))
        .addInterceptor(HttpLoggingInterceptor { message ->
            KotlinLogging.logger {}.debug { message }
        }.setLevel(BASIC))
        .build()

    val komgaClient = KomgaClient(
        client = HttpClient(
            client = komgaHttpClient,
            name = "Komga"
        ),
        moshi = jsonModule.moshi,
        baseUrl = komgaConfig.baseUri.toHttpUrl()
    )

    val komgaMediaServerClient = KomgaMediaServerClientAdapter(komgaClient)
    val komgaMetadataUpdateService = MetadataUpdateService(
        mediaServerClient = komgaMediaServerClient,
        seriesThumbnailsRepository = repositoryModule.seriesThumbnailsRepository,
        bookThumbnailsRepository = repositoryModule.bookThumbnailsRepository,
        metadataUpdateConfig = komgaConfig.metadataUpdate,
        metadataUpdateMapper = MetadataUpdateMapper(komgaConfig.metadataUpdate),
        comicInfoWriter = metadataModule.comicInfoWriter,
        serverType = KOMGA,
    )
    val komgaMetadataService = MetadataService(
        mediaServerClient = komgaMediaServerClient,
        metadataProviders = metadataModule.metadataProviders,
        aggregateMetadata = komgaConfig.aggregateMetadata,
        executor = metadataAggregationExecutor,
        metadataUpdateService = komgaMetadataUpdateService,
        seriesMatchRepository = repositoryModule.komgaSeriesMatchRepository
    )

    private val komgaNotificationService = NotificationService(
        mediaServerClient = komgaMediaServerClient,
        discordWebhookService = discordModule?.discordWebhookService,
        libraryFilter = {
            if (komgaConfig.notifications.libraries.isEmpty()) true
            else komgaConfig.notifications.libraries.contains(it)
        },
    )

    private val komgaEventListener = KomgaEventListener(
        client = komgaSseClient,
        moshi = jsonModule.moshi,
        komgaUrl = komgaConfig.baseUri.toHttpUrl(),
        metadataService = komgaMetadataService,
        libraryFilter = {
            if (komgaConfig.eventListener.libraries.isEmpty()) true
            else komgaConfig.eventListener.libraries.contains(it)
        },
        notificationService = komgaNotificationService,
        bookThumbnailsRepository = repositoryModule.bookThumbnailsRepository,
        seriesThumbnailsRepository = repositoryModule.seriesThumbnailsRepository,
        executor = eventListenerExecutor,
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
        .addInterceptor(HttpLoggingInterceptor { message ->
            KotlinLogging.logger {}.debug { message }
        }.setLevel(BASIC))
        .addInterceptor(KavitaBearerAuthInterceptor(kavitaTokenProvider))
        .build()

    private val kavitaClient = KavitaClient(
        client = HttpClient(
            client = kavitaHttpClient,
            name = "Kavita"
        ),
        moshi = jsonModule.moshi,
        baseUrl = kavitaConfig.baseUri.toHttpUrl()
    )
    val kavitaMediaServerClient = KavitaMediaServerClientAdapter(kavitaClient)
    val kavitaMetadataUpdateService = MetadataUpdateService(
        mediaServerClient = kavitaMediaServerClient,
        seriesThumbnailsRepository = repositoryModule.seriesThumbnailsRepository,
        bookThumbnailsRepository = repositoryModule.bookThumbnailsRepository,
        metadataUpdateConfig = kavitaConfig.metadataUpdate,
        metadataUpdateMapper = MetadataUpdateMapper(kavitaConfig.metadataUpdate),
        comicInfoWriter = metadataModule.comicInfoWriter,
        serverType = KAVITA,
    )
    val kavitaMetadataService = MetadataService(
        mediaServerClient = kavitaMediaServerClient,
        metadataProviders = metadataModule.metadataProviders,
        aggregateMetadata = kavitaConfig.aggregateMetadata,
        executor = metadataAggregationExecutor,
        metadataUpdateService = kavitaMetadataUpdateService,
        seriesMatchRepository = repositoryModule.kavitaSeriesMatchRepository
    )

    private val kavitaNotificationService = NotificationService(
        mediaServerClient = kavitaMediaServerClient,
        discordWebhookService = discordModule?.discordWebhookService,
        libraryFilter = {
            if (kavitaConfig.notifications.libraries.isEmpty()) true
            else kavitaConfig.notifications.libraries.contains(it)
        },
    )

    private val kavitaEventListener = KavitaEventListener(
        baseUrl = kavitaConfig.baseUri,
        metadataService = kavitaMetadataService,
        kavitaClient = kavitaClient,
        tokenProvider = kavitaTokenProvider,
        libraryFilter = {
            if (kavitaConfig.eventListener.libraries.isEmpty()) true
            else kavitaConfig.eventListener.libraries.contains(it)
        },
        notificationService = kavitaNotificationService,
        executor = eventListenerExecutor,
        clock = systemDefaultClock
    )

    init {
        if (komgaConfig.eventListener.enabled)
            komgaEventListener.start()
        if (kavitaConfig.eventListener.enabled)
            kavitaEventListener.start()

    }

    override fun close() {
        komgaEventListener.stop()
        kavitaEventListener.stop()
    }
}
