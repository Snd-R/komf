package org.snd.module

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.RetryConfig
import mu.KotlinLogging
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.snd.config.*
import org.snd.infra.HttpClient
import org.snd.infra.HttpException
import org.snd.metadata.NameMatchingMode
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.comicinfo.ComicInfoWriter
import org.snd.metadata.mylar.SeriesJsonWriter
import org.snd.metadata.providers.anilist.AniListClient
import org.snd.metadata.providers.anilist.AniListMetadataMapper
import org.snd.metadata.providers.anilist.AniListMetadataProvider
import org.snd.metadata.providers.bookwalker.BookWalkerClient
import org.snd.metadata.providers.bookwalker.BookWalkerMapper
import org.snd.metadata.providers.bookwalker.BookWalkerMetadataProvider
import org.snd.metadata.providers.kodansha.KodanshaClient
import org.snd.metadata.providers.kodansha.KodanshaMetadataMapper
import org.snd.metadata.providers.kodansha.KodanshaMetadataProvider
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
    private val config: MetadataProvidersConfig,
    private val okHttpClient: OkHttpClient,
    private val jsonModule: JsonModule
) {
    val comicInfoWriter = createComicInfoWriter()
    val seriesJsonWriter = SeriesJsonWriter(jsonModule.moshi)
    private val nameSimilarityMatcher = createNameSimilarityMatcher(config.nameMatchingMode)

    private val malClient = createMalClient(config.mal.clientId)
    private val mangaUpdatesClient = createMangaUpdatesClient()
    private val nautiljonClient = createNautiljonClient()
    private val aniListClient = createAnilistClient()
    private val yenPressClient = createYenPressClient()
    private val kodanshaClient = createKodanshaClient()
    private val vizClient = createVizClient()
    private val bookWalkerClient = createBookWalkerClient()

    private val malMetadataProvider = createMalMetadataProvider(config.mal, malClient)
    private val mangaUpdatesMetadataProvider = createMangaUpdatesMetadataProvider(config.mangaUpdates, mangaUpdatesClient)
    private val nautiljonMetadataProvider = createNautiljonMetadataProvider(config.nautiljon, nautiljonClient)
    private val aniListMetadataProvider = createAnilistMetadataProvider(config.aniList, aniListClient)
    private val yenPressMetadataProvider = createYenPressMetadataProvider(config.yenPress, yenPressClient)
    private val kodanshaMetadataProvider = createKodanshaMetadataProvider(config.kodansha, kodanshaClient)
    private val vizMetadataProvider = createVizMetadataProvider(config.viz, vizClient)
    private val bookWalkerMetadataProvider = createBookWalkerMetadataProvider(config.bookWalker, bookWalkerClient)


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


    private fun createComicInfoWriter() = ComicInfoWriter()
    private fun createNameSimilarityMatcher(mode: NameMatchingMode) = NameSimilarityMatcher.getInstance(mode)

    private fun createHttpClient(
        name: String,
        interceptors: Collection<Interceptor> = emptyList(),
        rateLimitConfig: RateLimiterConfig = RateLimiterConfig.ofDefaults(),
        retryConfig: RetryConfig = RetryConfig.ofDefaults(),
    ): HttpClient {
        val httpClient = okHttpClient.newBuilder()
            .addInterceptor(HttpLoggingInterceptor { message ->
                KotlinLogging.logger {}.debug { message }
            }.setLevel(HttpLoggingInterceptor.Level.BASIC))
        interceptors.forEach { httpClient.addInterceptor(it) }

        return HttpClient(
            client = httpClient.build(),
            name = name,
            rateLimiterConfig = rateLimitConfig,
            retryConfig = retryConfig
        )

    }

    private fun createMalClient(clientId: String): MalClient {
        val httpClient = createHttpClient(
            name = "MAL",
            interceptors = listOf(MalClientInterceptor(clientId)),
            rateLimitConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofSeconds(5))
                .build()
        )
        return MalClient(client = httpClient, moshi = jsonModule.moshi)
    }

    private fun createMangaUpdatesClient(): MangaUpdatesClient {
        val httpClient = createHttpClient(
            name = "MangaUpdates",
            rateLimitConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(5))
                .build()
        )

        return MangaUpdatesClient(httpClient, jsonModule.moshi)
    }

    private fun createNautiljonClient(): NautiljonClient {
        val retryConfig = RetryConfig.custom<Any>()
            .intervalBiFunction { _, result ->
                if (result.isRight) {
                    return@intervalBiFunction 5000
                }
                val exception = result.swap().get()
                return@intervalBiFunction if (exception is HttpException && exception.code == 429) {
                    exception.headers["retry-after"]?.toLong()?.times(1000) ?: 5000
                } else 5000
            }.build()

        val httpClient = createHttpClient(
            name = "Nautiljon",
            rateLimitConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(3)
                .timeoutDuration(Duration.ofSeconds(5))
                .build(),
            retryConfig = retryConfig
        )

        return NautiljonClient(httpClient)
    }

    private fun createAnilistClient(): AniListClient {
        return AniListClient(
            okHttpClient = okHttpClient.newBuilder().build(),
            name = "AniList",
            rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(5))
                .build()
        )
    }

    private fun createYenPressClient(): YenPressClient {
        return YenPressClient(
            createHttpClient(
                name = "YenPress",
                rateLimitConfig = RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofSeconds(5))
                    .limitForPeriod(5)
                    .timeoutDuration(Duration.ofSeconds(5))
                    .build()
            )
        )
    }

    private fun createKodanshaClient(): KodanshaClient {
        return KodanshaClient(
            createHttpClient(
                name = "Kodansha",
                rateLimitConfig = RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofSeconds(5))
                    .limitForPeriod(5)
                    .timeoutDuration(Duration.ofSeconds(5))
                    .build()
            )
        )
    }

    private fun createVizClient(): VizClient {
        return VizClient(
            createHttpClient(
                name = "Viz",
                rateLimitConfig = RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofSeconds(5))
                    .limitForPeriod(3)
                    .timeoutDuration(Duration.ofSeconds(5))
                    .build(),
                retryConfig = RetryConfig.custom<Any>().intervalFunction { 5000 }.build()
            )
        )
    }

    private fun createBookWalkerClient(): BookWalkerClient {
        return BookWalkerClient(
            createHttpClient(
                name = "BookWalker",
                rateLimitConfig = RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofSeconds(5))
                    .limitForPeriod(5)
                    .timeoutDuration(Duration.ofSeconds(5))
                    .build(),
                retryConfig = RetryConfig.custom<Any>()
                    .ignoreExceptions(HttpException.NotFound::class.java)
                    .intervalFunction { 5000 }
                    .build()
            )
        )
    }


    private fun createMalMetadataProvider(
        config: MalConfig,
        client: MalClient,
    ): MalMetadataProvider? {
        if (config.enabled.not()) return null

        val malMetadataMapper = MalMetadataMapper(config.seriesMetadata)
        val malSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return MalMetadataProvider(client, malMetadataMapper, malSimilarityMatcher)
    }

    private fun createMangaUpdatesMetadataProvider(
        config: MangaUpdatesConfig,
        client: MangaUpdatesClient,
    ): MangaUpdatesMetadataProvider? {
        if (config.enabled.not()) return null

        val mangaUpdatesMetadataMapper = MangaUpdatesMetadataMapper(config.seriesMetadata)
        val mangaUpdatesSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return MangaUpdatesMetadataProvider(client, mangaUpdatesMetadataMapper, mangaUpdatesSimilarityMatcher)
    }

    private fun createNautiljonMetadataProvider(config: NautiljonConfig, client: NautiljonClient): NautiljonMetadataProvider? {
        if (config.enabled.not()) return null
        val seriesMetadataMapper = NautiljonSeriesMetadataMapper(
            config.seriesMetadata,
            config.bookMetadata,
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return NautiljonMetadataProvider(client, seriesMetadataMapper, similarityMatcher)
    }

    private fun createAnilistMetadataProvider(config: AniListConfig, client: AniListClient): AniListMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = AniListMetadataMapper(config.seriesMetadata)
        val similarityMatcher = config.nameMatchingMode
            ?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return AniListMetadataProvider(client, metadataMapper, similarityMatcher)
    }

    private fun createYenPressMetadataProvider(config: YenPressConfig, client: YenPressClient): YenPressMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = YenPressMetadataMapper(config.seriesMetadata, config.bookMetadata)
        val similarityMatcher = config.nameMatchingMode
            ?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return YenPressMetadataProvider(client, metadataMapper, similarityMatcher)
    }

    private fun createKodanshaMetadataProvider(config: KodanshaConfig, client: KodanshaClient): KodanshaMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = KodanshaMetadataMapper(config.seriesMetadata, config.bookMetadata)
        val similarityMatcher =
            config.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher

        return KodanshaMetadataProvider(client, metadataMapper, similarityMatcher)
    }

    private fun createVizMetadataProvider(config: VizConfig, client: VizClient): VizMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = VizMetadataMapper(config.seriesMetadata, config.bookMetadata)
        val similarityMatcher = config.nameMatchingMode
            ?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher

        return VizMetadataProvider(client, metadataMapper, similarityMatcher)
    }

    private fun createBookWalkerMetadataProvider(config: BookWalkerConfig, client: BookWalkerClient): BookWalkerMetadataProvider? {
        if (config.enabled.not()) return null

        val bookWalkerMapper = BookWalkerMapper(config.seriesMetadata, config.bookMetadata)
        val similarityMatcher = config.nameMatchingMode
            ?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher

        return BookWalkerMetadataProvider(client, bookWalkerMapper, similarityMatcher)
    }
}
