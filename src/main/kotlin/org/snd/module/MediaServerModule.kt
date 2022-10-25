package org.snd.module

import mu.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import org.snd.config.KomgaConfig
import org.snd.infra.BasicAuthInterceptor
import org.snd.infra.HttpClient
import org.snd.infra.SimpleCookieJar
import org.snd.mediaserver.MetadataService
import org.snd.mediaserver.MetadataUpdateMapper
import org.snd.mediaserver.NotificationService
import org.snd.mediaserver.komga.KomgaClient
import org.snd.mediaserver.komga.KomgaEventListener
import org.snd.mediaserver.komga.KomgaMediaServerClientAdapter
import org.snd.mediaserver.model.MediaServer.KOMGA
import org.snd.metadata.comicinfo.ComicInfoWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

class MediaServerModule(
    config: KomgaConfig,
    okHttpClient: OkHttpClient,
    jsonModule: JsonModule,
    repositoryModule: RepositoryModule,
    metadataModule: MetadataModule,
    discordModule: DiscordModule?,
) : AutoCloseable {
    private val executor = Executors.newFixedThreadPool(4)

    private val httpClient = okHttpClient.newBuilder()
        .cookieJar(SimpleCookieJar())
        .build()

    private val komgaHttpClient = httpClient
        .newBuilder()
        .readTimeout(0, SECONDS)
        .addInterceptor(BasicAuthInterceptor(config.komgaUser, config.komgaPassword))
        .addInterceptor(HttpLoggingInterceptor { message ->
            KotlinLogging.logger {}.debug { message }
        }.setLevel(BASIC))
        .build()

    private val komgaSseClient = httpClient.newBuilder()
        .readTimeout(0, SECONDS)
        .addInterceptor(BasicAuthInterceptor(config.komgaUser, config.komgaPassword))
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
        baseUrl = config.baseUri.toHttpUrl()
    )

    val komgaMediaServerClient = if (config.enabled) KomgaMediaServerClientAdapter(komgaClient) else null
    val komgaMetadataService = komgaMediaServerClient?.let {
        MetadataService(
            mediaServerClient = komgaMediaServerClient,
            metadataProviders = metadataModule.metadataProviders,
            matchedSeriesRepository = repositoryModule.matchedSeriesRepository,
            matchedBookRepository = repositoryModule.matchedBookRepository,
            metadataUpdateConfig = config.metadataUpdate,
            metadataUpdateMapper = MetadataUpdateMapper(config.metadataUpdate),
            aggregateMetadata = config.aggregateMetadata,
            executor = executor,
            comicInfoWriter = ComicInfoWriter(),
            serverType = KOMGA
        )
    }

    private val komgaNotificationService = NotificationService(
        KomgaMediaServerClientAdapter(komgaClient),
        discordModule?.discordWebhookService
    )

    private val komgaEventListener = komgaMetadataService?.let {
        KomgaEventListener(
            client = komgaSseClient,
            moshi = jsonModule.moshi,
            komgaUrl = config.baseUri.toHttpUrl(),
            metadataService = komgaMetadataService,
            libraryFilter = {
                if (config.eventListener.libraries.isEmpty()) true
                else config.eventListener.libraries.contains(it)
            },
            notificationService = komgaNotificationService,
            matchedBookRepository = repositoryModule.matchedBookRepository,
            matchedSeriesRepository = repositoryModule.matchedSeriesRepository
        )
    }

    init {
        if (config.eventListener.enabled)
            komgaEventListener?.start()
    }

    override fun close() {
        komgaEventListener?.stop()
    }
}
