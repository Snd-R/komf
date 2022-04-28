package org.snd.module

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.RetryConfig
import mu.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import org.snd.config.KomgaConfig
import org.snd.infra.BasicAuthInterceptor
import org.snd.infra.HttpClient
import org.snd.infra.HttpException
import org.snd.infra.SimpleCookieJar
import org.snd.komga.KomgaClient
import org.snd.komga.KomgaEventListener
import org.snd.komga.KomgaService
import org.snd.komga.MetadataUpdateMapper
import org.snd.komga.webhook.DiscordClient
import org.snd.komga.webhook.DiscordWebhooks
import java.time.Duration
import java.util.concurrent.TimeUnit.SECONDS

class KomgaModule(
    config: KomgaConfig,
    jsonModule: JsonModule,
    repositoryModule: RepositoryModule,
    metadataModule: MetadataModule
) : AutoCloseable {
    private val httpClient = OkHttpClient.Builder()
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

    private val discordHttpClient = httpClient
        .newBuilder()
        .addInterceptor(HttpLoggingInterceptor { message ->
            KotlinLogging.logger {}.debug { message }
        }.setLevel(BASIC))
        .build()

    private val discordRetryConfig = RetryConfig.custom<Any>()
        .intervalBiFunction { _, result ->
            if (result.isRight) {
                return@intervalBiFunction 5000
            }
            val exception = result.swap().get()
            return@intervalBiFunction if (exception is HttpException && exception.code == 429) {
                exception.headers["retry-after"]?.toLong() ?: 5000
            } else 5000
        }.build()

    private val komgaClient = KomgaClient(
        client = HttpClient(
            client = komgaHttpClient,
            name = "Komga"
        ),
        moshi = jsonModule.moshi,
        baseUrl = config.baseUri.toHttpUrl()
    )

    val komgaService = KomgaService(
        komgaClient = komgaClient,
        metadataProviders = metadataModule.metadataProviders,
        matchedSeriesRepository = repositoryModule.matchedSeriesRepository,
        matchedBookRepository = repositoryModule.matchedBookRepository,
        config.metadataUpdate,
        MetadataUpdateMapper(config.metadataUpdate)
    )

    private val discordWebhooks = config.webhooks?.let { webhooks ->
        DiscordWebhooks(
            webhooks,
            komgaClient,
            DiscordClient(
                client = HttpClient(
                    client = discordHttpClient,
                    name = "Discord",
                    rateLimiterConfig = RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(2))
                        .limitForPeriod(4)
                        .timeoutDuration(Duration.ofSeconds(2))
                        .build(),
                    retryConfig = discordRetryConfig
                ),
                moshi = jsonModule.moshi
            )
        )
    }

    private val komgaEventListener = KomgaEventListener(
        client = komgaSseClient,
        moshi = jsonModule.moshi,
        komgaUrl = config.baseUri.toHttpUrl(),
        komgaService = komgaService,
        libraryFilter = {
            if (config.eventListener.libraries.isEmpty()) true
            else config.eventListener.libraries.contains(it)
        },
        discordWebhooks = discordWebhooks,
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
