package org.snd.module

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.RetryConfig
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.snd.config.MetadataProvidersConfig
import org.snd.infra.HttpClient
import org.snd.infra.HttpException
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.providers.anilist.AniListClient
import org.snd.metadata.providers.anilist.AniListMetadataMapper
import org.snd.metadata.providers.anilist.AniListMetadataProvider
import org.snd.metadata.providers.bookwalker.BookWalkerClient
import org.snd.metadata.providers.bookwalker.BookWalkerMapper
import org.snd.metadata.providers.bookwalker.BookWalkerMetadataProvider
import org.snd.metadata.providers.kodansha.KodanshaClient
import org.snd.metadata.providers.kodansha.KodanshaMetadataMapper
import org.snd.metadata.providers.kodansha.KondanshaMetadataProvider
import org.snd.metadata.providers.mal.MalClient
import org.snd.metadata.providers.mal.MalClientInterceptor
import org.snd.metadata.providers.mal.MalMetadataMapper
import org.snd.metadata.providers.mal.MalMetadataProvider
import org.snd.metadata.providers.mangaupdates.MangaUpdatesClient
import org.snd.metadata.providers.mangaupdates.MangaUpdatesMetadataMapper
import org.snd.metadata.providers.mangaupdates.MangaUpdatesMetadataProvider
import org.snd.metadata.providers.nautiljon.NautiljonClient
import org.snd.metadata.providers.nautiljon.NautiljonMetadataProvider
import org.snd.metadata.providers.nautiljon.NautiljonSeriesMetadataMapper
import org.snd.metadata.providers.viz.VizClient
import org.snd.metadata.providers.viz.VizMetadataMapper
import org.snd.metadata.providers.viz.VizMetadataProvider
import org.snd.metadata.providers.yenpress.YenPressClient
import org.snd.metadata.providers.yenpress.YenPressMetadataMapper
import org.snd.metadata.providers.yenpress.YenPressMetadataProvider
import java.time.Duration


class MetadataModule(
    config: MetadataProvidersConfig,
    okHttpClient: OkHttpClient,
    jsonModule: JsonModule
) {
    private val nameSimilarityMatcher = NameSimilarityMatcher.getInstance(config.nameMatchingMode)

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
    private val malSimilarityMatcher: NameSimilarityMatcher =
        config.mal.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
    private val malMetadataProvider = malClient?.let { MalMetadataProvider(it, malMetadataMapper, malSimilarityMatcher) }

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
    private val mangaUpdatesSimilarityMatcher: NameSimilarityMatcher =
        config.mangaUpdates.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
    private val mangaUpdatesMetadataProvider =
        mangaUpdatesClient?.let {
            println()
            MangaUpdatesMetadataProvider(it, mangaUpdatesMetadataMapper, mangaUpdatesSimilarityMatcher)
        }

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

    private val nautiljonSimilarityMatcher = config.nautiljon.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
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
                nautiljonSimilarityMatcher
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
    private val aniListSimilarityMatcher = config.aniList.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
    private val aniListMetadataProvider = config.aniList.let {
        if (it.enabled) {
            AniListMetadataProvider(aniListClient, aniListMetadataMapper, aniListSimilarityMatcher)
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
    private val yenPressSimilarityMatcher = config.yenPress.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
    private val yenPressMetadataProvider = config.yenPress.let {
        if (it.enabled) YenPressMetadataProvider(yenPressClient, yenPressMetadataMapper, yenPressSimilarityMatcher)
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
    private val kodanshaSimilarityMatcher = config.kodansha.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
    private val kodanshaMetadataProvider = config.kodansha.let {
        if (it.enabled) KondanshaMetadataProvider(kodanshaClient, kodanshaMetadataMapper, kodanshaSimilarityMatcher)
        else null
    }

    private val vizClient = VizClient(
        HttpClient(
            client = httpClient.newBuilder().build(),
            name = "Viz",
            rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(3)
                .timeoutDuration(Duration.ofSeconds(5))
                .build(),
            retryConfig = RetryConfig.custom<Any>().intervalFunction { 5000 }.build()
        )
    )

    private val vizMetadataMapper = VizMetadataMapper(
        seriesMetadataConfig = config.viz.seriesMetadata,
        bookMetadataConfig = config.viz.bookMetadata
    )
    private val vizSimilarityMatcher = config.viz.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
    private val vizMetadataProvider = config.viz.let {
        if (it.enabled) VizMetadataProvider(vizClient, vizMetadataMapper, vizSimilarityMatcher)
        else null
    }

    private val bookWalkerClient = BookWalkerClient(
        HttpClient(
            client = httpClient.newBuilder().build(),
            name = "BookWalker",
            rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(5))
                .build(),
            retryConfig = RetryConfig.custom<Any>().intervalFunction { 5000 }.build()
        )
    )
    private val bookWalkerMapper = BookWalkerMapper(
        seriesMetadataConfig = config.bookWalker.seriesMetadata,
        bookMetadataConfig = config.bookWalker.bookMetadata
    )

    private val bookWalkerSimilarityMatcher = config.bookWalker.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
    private val bookWalkerMetadataProvider = config.bookWalker.let {
        if (it.enabled) BookWalkerMetadataProvider(bookWalkerClient, bookWalkerMapper, bookWalkerSimilarityMatcher)
        else null
    }

    val metadataProviders = listOfNotNull(
        malMetadataProvider?.let { it to config.mal.priority },
        mangaUpdatesMetadataProvider?.let { it to config.mangaUpdates.priority },
        nautiljonMetadataProvider?.let { it to config.nautiljon.priority },
        aniListMetadataProvider?.let { it to config.aniList.priority },
        yenPressMetadataProvider?.let { it to config.yenPress.priority },
        kodanshaMetadataProvider?.let { it to config.kodansha.priority },
        vizMetadataProvider?.let { it to config.viz.priority },
        bookWalkerMetadataProvider?.let { it to config.bookWalker.priority }
    )
        .sortedBy { (_, priority) -> priority }
        .associate { (provider, _) -> provider.providerName() to provider }
}
