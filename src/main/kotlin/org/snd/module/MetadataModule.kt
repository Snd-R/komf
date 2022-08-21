package org.snd.module

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.RetryConfig
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.snd.config.MetadataProvidersConfig
import org.snd.infra.HttpClient
import org.snd.infra.HttpException
import org.snd.metadata.anilist.AniListClient
import org.snd.metadata.anilist.AniListMetadataMapper
import org.snd.metadata.anilist.AniListMetadataProvider
import org.snd.metadata.kodansha.KodanshaClient
import org.snd.metadata.kodansha.KodanshaMetadataMapper
import org.snd.metadata.kodansha.KondanshaMetadataProvider
import org.snd.metadata.mal.MalClient
import org.snd.metadata.mal.MalClientInterceptor
import org.snd.metadata.mal.MalMetadataMapper
import org.snd.metadata.mal.MalMetadataProvider
import org.snd.metadata.mangaupdates.MangaUpdatesClient
import org.snd.metadata.mangaupdates.MangaUpdatesMetadataMapper
import org.snd.metadata.mangaupdates.MangaUpdatesMetadataProvider
import org.snd.metadata.nautiljon.NautiljonClient
import org.snd.metadata.nautiljon.NautiljonMetadataProvider
import org.snd.metadata.nautiljon.NautiljonSeriesMetadataMapper
import org.snd.metadata.yenpress.YenPressClient
import org.snd.metadata.yenpress.YenPressMetadataMapper
import org.snd.metadata.yenpress.YenPressMetadataProvider
import java.time.Duration


class MetadataModule(
    config: MetadataProvidersConfig,
    okHttpClient: OkHttpClient,
    jsonModule: JsonModule
) {
    private val httpClient = okHttpClient.newBuilder()
        .addInterceptor(HttpLoggingInterceptor { message ->
            KotlinLogging.logger {}.debug { message }
        }.setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build()

    private val malHttpClient = config.mal.let {
        if (it.enabled)
            HttpClient(
                client = httpClient.newBuilder()
                    .addInterceptor(MalClientInterceptor(it.clientId))
                    .build(),
                name = "MAL",
                rateLimiterConfig = RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofSeconds(5))
                    .limitForPeriod(10)
                    .timeoutDuration(Duration.ofSeconds(5))
                    .build()
            )
        else null
    }

    private val malClient = malHttpClient?.let { MalClient(client = it, moshi = jsonModule.moshi) }
    private val malMetadataMapper = MalMetadataMapper(config.mal.seriesMetadata)
    private val malMetadataProvider = malClient?.let { MalMetadataProvider(it, malMetadataMapper) }

    private val mangaUpdatesClient = config.mangaUpdates.let {
        if (it.enabled)
            MangaUpdatesClient(
                HttpClient(
                    client = httpClient.newBuilder().build(),
                    name = "MangaUpdates",
                    rateLimiterConfig = RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(5))
                        .limitForPeriod(5)
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build()
                ),
                jsonModule.moshi
            )
        else null
    }
    private val mangaUpdatesMetadataMapper = MangaUpdatesMetadataMapper(config.mangaUpdates.seriesMetadata)
    private val mangaUpdatesMetadataProvider = mangaUpdatesClient?.let { MangaUpdatesMetadataProvider(it, mangaUpdatesMetadataMapper) }

    private val nautiljonSeriesMetadataMapper = NautiljonSeriesMetadataMapper(
        config.nautiljon.useOriginalPublisher,
        config.nautiljon.originalPublisherTag,
        config.nautiljon.frenchPublisherTag,
        config.nautiljon.seriesMetadata,
        config.nautiljon.bookMetadata,
    )

    private val nautiljonRetryConfig = RetryConfig.custom<Any>()
        .intervalBiFunction { _, result ->
            if (result.isRight) {
                return@intervalBiFunction 5000
            }
            val exception = result.swap().get()
            return@intervalBiFunction if (exception is HttpException && exception.code == 429) {
                exception.headers["retry-after"]?.toLong()?.times(1000) ?: 5000
            } else 5000
        }.build()

    private val nautiljonMetadataProvider = config.nautiljon.let {
        if (it.enabled) {
            NautiljonMetadataProvider(
                NautiljonClient(
                    HttpClient(
                        client = httpClient.newBuilder().build(),
                        name = "nautiljon",
                        rateLimiterConfig = RateLimiterConfig.custom()
                            .limitRefreshPeriod(Duration.ofSeconds(5))
                            .limitForPeriod(3)
                            .timeoutDuration(Duration.ofSeconds(5))
                            .build(),
                        nautiljonRetryConfig
                    )
                ),
                nautiljonSeriesMetadataMapper,
            )
        } else null
    }

    private val aniListClient = AniListClient(
        okHttpClient = httpClient.newBuilder().build(),
        name = "AniList",
        rateLimiterConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(5))
            .limitForPeriod(5)
            .timeoutDuration(Duration.ofSeconds(5))
            .build()
    )
    private val aniListMetadataMapper = AniListMetadataMapper(config.aniList.seriesMetadata)

    private val aniListMetadataProvider = config.aniList.let {
        if (it.enabled) {
            AniListMetadataProvider(aniListClient, aniListMetadataMapper)
        } else null
    }

    private val yenPressClient: YenPressClient = YenPressClient(
        HttpClient(
            client = httpClient.newBuilder().build(),
            name = "YenPress",
            rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(5))
                .build()
        )
    )
    private val yenPressMetadataMapper = YenPressMetadataMapper(config.yenPress.seriesMetadata, config.yenPress.bookMetadata)
    private val yenPressMetadataProvider = config.yenPress.let {
        if (it.enabled) YenPressMetadataProvider(yenPressClient, yenPressMetadataMapper)
        else null
    }

    private val kodanshaClient: KodanshaClient = KodanshaClient(
        HttpClient(
            client = httpClient.newBuilder().build(),
            name = "Kodansha",
            rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(5))
                .build()
        )
    )
    private val kodanshaMetadataMapper: KodanshaMetadataMapper = KodanshaMetadataMapper(
        seriesMetadataConfig = config.kodansha.seriesMetadata,
        bookMetadataConfig = config.kodansha.bookMetadata,
    )
    private val kodanshaMetadataProvider = config.kodansha.let {
        if (it.enabled) KondanshaMetadataProvider(kodanshaClient, kodanshaMetadataMapper)
        else null

    }

    val metadataProviders = listOfNotNull(
        malMetadataProvider?.let { it to config.mal.priority },
        mangaUpdatesMetadataProvider?.let { it to config.mangaUpdates.priority },
        nautiljonMetadataProvider?.let { it to config.nautiljon.priority },
        aniListMetadataProvider?.let { it to config.aniList.priority },
        yenPressMetadataProvider?.let { it to config.yenPress.priority },
        kodanshaMetadataProvider?.let { it to config.kodansha.priority }
    )
        .sortedBy { (_, priority) -> priority }
        .associate { (provider, _) -> provider.providerName() to provider }
}
