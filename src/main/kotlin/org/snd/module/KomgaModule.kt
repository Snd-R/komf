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
import org.snd.komga.KomgaClient
import org.snd.komga.KomgaEventListener
import org.snd.komga.KomgaMetadataService
import org.snd.komga.KomgaNotificationService
import org.snd.komga.MetadataUpdateMapper
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

class KomgaModule(
    config: KomgaConfig,
    okHttpClient: OkHttpClient,
    jsonModule: JsonModule,
    repositoryModule: RepositoryModule,
    metadataModule: MetadataModule,
    discordModule: DiscordModule?,
) : AutoCloseable {
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

    val komgaMetadataService = KomgaMetadataService(
        komgaClient = komgaClient,
        metadataProviders = metadataModule.metadataProviders,
        matchedSeriesRepository = repositoryModule.matchedSeriesRepository,
        matchedBookRepository = repositoryModule.matchedBookRepository,
        metadataUpdateConfig = config.metadataUpdate,
        metadataUpdateMapper = MetadataUpdateMapper(config.metadataUpdate),
        aggregateMetadata = config.aggregateMetadata,
        executor = Executors.newFixedThreadPool(4)
    )

    private val notificationService = KomgaNotificationService(
        komgaClient,
        discordModule?.discordWebhookService
    )

    private val komgaEventListener = KomgaEventListener(
        client = komgaSseClient,
        moshi = jsonModule.moshi,
        komgaUrl = config.baseUri.toHttpUrl(),
        komgaMetadataService = komgaMetadataService,
        libraryFilter = {
            if (config.eventListener.libraries.isEmpty()) true
            else config.eventListener.libraries.contains(it)
        },
        notificationService = notificationService,
        matchedBookRepository = repositoryModule.matchedBookRepository,
        matchedSeriesRepository = repositoryModule.matchedSeriesRepository
    )

    init {
        if (config.eventListener.enabled)
            komgaEventListener.start()
    }

    override fun close() {
        komgaEventListener.stop()
    }
}
