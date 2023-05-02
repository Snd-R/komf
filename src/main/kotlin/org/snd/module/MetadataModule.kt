package org.snd.module

import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.RetryConfig
import mu.KotlinLogging
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.apache.commons.lang3.SystemUtils
import org.snd.common.exceptions.HttpException
import org.snd.common.exceptions.ValidationException
import org.snd.common.http.HttpClient
import org.snd.config.AniListConfig
import org.snd.config.CalibreConfig
import org.snd.config.MetadataProvidersConfig
import org.snd.config.ProviderConfig
import org.snd.config.ProvidersConfig
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.comicinfo.ComicInfoWriter
import org.snd.metadata.epub.CalibreEpubMetadataWriter
import org.snd.metadata.model.NameMatchingMode
import org.snd.metadata.model.Provider
import org.snd.metadata.providers.anilist.AniListClient
import org.snd.metadata.providers.anilist.AniListMetadataMapper
import org.snd.metadata.providers.anilist.AniListMetadataProvider
import org.snd.metadata.providers.bangumi.BangumiClient
import org.snd.metadata.providers.bangumi.BangumiMetadataMapper
import org.snd.metadata.providers.bangumi.BangumiMetadataProvider
import org.snd.metadata.providers.bangumi.BangumiUserAgentInterceptor
import org.snd.metadata.providers.bookwalker.BookWalkerClient
import org.snd.metadata.providers.bookwalker.BookWalkerMapper
import org.snd.metadata.providers.bookwalker.BookWalkerMetadataProvider
import org.snd.metadata.providers.comicvine.ComicVineApiKeyInterceptor
import org.snd.metadata.providers.comicvine.ComicVineClient
import org.snd.metadata.providers.comicvine.ComicVineMetadataMapper
import org.snd.metadata.providers.comicvine.ComicVineMetadataProvider
import org.snd.metadata.providers.kodansha.KodanshaClient
import org.snd.metadata.providers.kodansha.KodanshaMetadataMapper
import org.snd.metadata.providers.kodansha.KodanshaMetadataProvider
import org.snd.metadata.providers.mal.MalClient
import org.snd.metadata.providers.mal.MalClientInterceptor
import org.snd.metadata.providers.mal.MalMetadataMapper
import org.snd.metadata.providers.mal.MalMetadataProvider
import org.snd.metadata.providers.mangadex.MangaDexClient
import org.snd.metadata.providers.mangadex.MangaDexMetadataMapper
import org.snd.metadata.providers.mangadex.MangaDexMetadataProvider
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
import java.lang.ProcessBuilder.Redirect
import java.nio.file.Path
import java.time.Duration


class MetadataModule(
    providersConfig: MetadataProvidersConfig,
    calibreConfig: CalibreConfig,
    private val okHttpClient: OkHttpClient,
    private val jsonModule: JsonModule
) : AutoCloseable {
    val comicInfoWriter = createComicInfoWriter()
    val epubWriter = createEpubWriter(calibreConfig)
    private val nameSimilarityMatcher = createNameSimilarityMatcher(providersConfig.nameMatchingMode)

    private val malClient = createMalClient(providersConfig.malClientId)
    private val mangaUpdatesClient = createMangaUpdatesClient()
    private val nautiljonClient = createNautiljonClient()
    private val aniListClient = createAnilistClient()
    private val yenPressClient = createYenPressClient()
    private val kodanshaClient = createKodanshaClient()
    private val vizClient = createVizClient()
    private val bookWalkerClient = createBookWalkerClient()
    private val mangaDexClient = createMangaDexClient()
    private val bangumiClient = createBangumiClient()
    private val comicVineClient = providersConfig.comicVineApiKey?.let { createComicVineClient(it) }

    val metadataProviders = createMetadataProviders(providersConfig)

    private fun createMetadataProviders(config: MetadataProvidersConfig): MetadataProviders {
        val defaultProviders = createMetadataProviders(config.defaultProviders)
        val libraryProviders = config.libraryProviders
            .map { (libraryId, config) -> libraryId to createMetadataProviders(config) }
            .toMap()

        return MetadataProviders(defaultProviders, libraryProviders)
    }

    private fun createMetadataProviders(config: ProvidersConfig) = MetadataProvidersContainer(
        mangaupdates = createMangaUpdatesMetadataProvider(config.mangaUpdates, mangaUpdatesClient),
        mangaupdatesPriority = config.mangaUpdates.priority,
        mal = createMalMetadataProvider(config.mal, malClient),
        malPriority = config.mal.priority,
        nautiljon = createNautiljonMetadataProvider(config.nautiljon, nautiljonClient),
        nautiljonPriority = config.nautiljon.priority,
        anilist = createAnilistMetadataProvider(config.aniList, aniListClient),
        anilistPriority = config.aniList.priority,
        yenPress = createYenPressMetadataProvider(config.yenPress, yenPressClient),
        yenPressPriority = config.yenPress.priority,
        kodansha = createKodanshaMetadataProvider(config.kodansha, kodanshaClient),
        kodanshaPriority = config.kodansha.priority,
        viz = createVizMetadataProvider(config.viz, vizClient),
        vizPriority = config.viz.priority,
        bookwalker = createBookWalkerMetadataProvider(config.bookWalker, bookWalkerClient),
        bookwalkerPriority = config.bookWalker.priority,
        mangaDex = createMangaDexMetadataProvider(config.mangaDex, mangaDexClient),
        mangaDexPriority = config.mangaDex.priority,
        bangumi = createBangumiMetadataProvider(config.bangumi, bangumiClient),
        bangumiPriority = config.bangumi.priority,
        comicVine = createComicVineMetadataProvider(config.comicVine, comicVineClient),
        comicVinePriority = config.comicVine.priority
    )

    private fun createComicInfoWriter() = ComicInfoWriter()
    private fun createEpubWriter(config: CalibreConfig): CalibreEpubMetadataWriter {
        val executablePath = when {
            config.ebookMetaPath != null -> config.ebookMetaPath
            SystemUtils.IS_OS_WINDOWS -> executeCommandAndReturnOutput("where", "ebook-meta.exe")
            else -> executeCommandAndReturnOutput("which", "ebook-meta")

        }?.let { Path.of(it.trim()) }

        return CalibreEpubMetadataWriter(executablePath)
    }

    private fun executeCommandAndReturnOutput(vararg command: String) = runCatching {
        val proc = ProcessBuilder(*command)
            .redirectOutput(Redirect.PIPE)
            .redirectError(Redirect.PIPE)
            .start()
        proc.waitFor()
        proc.inputStream.bufferedReader().readText()
    }.getOrNull()

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
                .limitForPeriod(5)
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
        val okHttpClient = okHttpClient.newBuilder()
            .addInterceptor(HttpLoggingInterceptor { message ->
                KotlinLogging.logger {}.debug { message }
            }.setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()

        return AniListClient(
            okHttpClient = okHttpClient,
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
            client = createHttpClient(
                name = "YenPress",
                rateLimitConfig = RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofSeconds(5))
                    .limitForPeriod(5)
                    .timeoutDuration(Duration.ofSeconds(5))
                    .build()
            ),
            moshi = jsonModule.moshi
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
            ),
            moshi = jsonModule.moshi
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
                retryConfig = RetryConfig.custom<Any>()
                    .maxAttempts(3)
                    .ignoreExceptions(HttpException.NotFound::class.java)
                    .intervalFunction(IntervalFunction.ofExponentialBackoff(5000, 2.0))
                    .build()
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

    private fun createMangaDexClient(): MangaDexClient {
        val httpClient = createHttpClient(
            name = "MangaDex",
            rateLimitConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(5))
                .build()
        )

        return MangaDexClient(httpClient, jsonModule.moshi)
    }

    private fun createBangumiClient(): BangumiClient {
        val httpClient = createHttpClient(
            name = "Bangumi",
            rateLimitConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(5))
                .build(),
            interceptors = listOf(BangumiUserAgentInterceptor())
        )

        return BangumiClient(httpClient, jsonModule.moshi)
    }

    private fun createComicVineClient(apiKey: String): ComicVineClient {
        val httpClient = createHttpClient(
            name = "ComicVine",
            interceptors = listOf(ComicVineApiKeyInterceptor(apiKey)),
            rateLimitConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(5))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(5))
                .build()
        )

        return ComicVineClient(httpClient, jsonModule.moshi)
    }

    private fun createMalMetadataProvider(
        config: ProviderConfig,
        client: MalClient,
    ): MalMetadataProvider? {
        if (config.enabled.not()) return null

        val malMetadataMapper = MalMetadataMapper(
            metadataConfig = config.seriesMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val malSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return MalMetadataProvider(
            client,
            malMetadataMapper,
            malSimilarityMatcher,
            config.seriesMetadata.thumbnail,
            config.mediaType,
        )
    }

    private fun createMangaUpdatesMetadataProvider(
        config: ProviderConfig,
        client: MangaUpdatesClient,
    ): MangaUpdatesMetadataProvider? {
        if (config.enabled.not()) return null

        val mangaUpdatesMetadataMapper = MangaUpdatesMetadataMapper(
            metadataConfig = config.seriesMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val mangaUpdatesSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return MangaUpdatesMetadataProvider(
            client,
            mangaUpdatesMetadataMapper,
            mangaUpdatesSimilarityMatcher,
            config.seriesMetadata.thumbnail,
            config.mediaType
        )
    }

    private fun createNautiljonMetadataProvider(config: ProviderConfig, client: NautiljonClient): NautiljonMetadataProvider? {
        if (config.enabled.not()) return null
        val seriesMetadataMapper = NautiljonSeriesMetadataMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return NautiljonMetadataProvider(
            client,
            seriesMetadataMapper,
            similarityMatcher,
            config.seriesMetadata.thumbnail,
            config.bookMetadata.thumbnail,
        )
    }

    private fun createAnilistMetadataProvider(config: AniListConfig, client: AniListClient): AniListMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = AniListMetadataMapper(
            metadataConfig = config.seriesMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
            tagsSizeLimit = config.tagsSizeLimit,
            tagsScoreThreshold = config.tagsScoreThreshold
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return AniListMetadataProvider(
            client,
            metadataMapper,
            similarityMatcher,
            config.seriesMetadata.thumbnail,
            config.mediaType
        )
    }

    private fun createYenPressMetadataProvider(config: ProviderConfig, client: YenPressClient): YenPressMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = YenPressMetadataMapper(
            config.seriesMetadata,
            config.bookMetadata,
            config.authorRoles,
            config.artistRoles
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return YenPressMetadataProvider(
            client,
            metadataMapper,
            similarityMatcher,
            config.mediaType,
            config.seriesMetadata.thumbnail,
            config.bookMetadata.thumbnail,
        )
    }

    private fun createKodanshaMetadataProvider(config: ProviderConfig, client: KodanshaClient): KodanshaMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = KodanshaMetadataMapper(config.seriesMetadata, config.bookMetadata)
        val similarityMatcher =
            config.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher

        return KodanshaMetadataProvider(
            client,
            metadataMapper,
            similarityMatcher,
            config.seriesMetadata.thumbnail,
            config.bookMetadata.thumbnail,
        )
    }

    private fun createVizMetadataProvider(config: ProviderConfig, client: VizClient): VizMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = VizMetadataMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher

        return VizMetadataProvider(
            client,
            metadataMapper,
            similarityMatcher,
            config.seriesMetadata.thumbnail,
            config.bookMetadata.thumbnail,
        )
    }

    private fun createBookWalkerMetadataProvider(config: ProviderConfig, client: BookWalkerClient): BookWalkerMetadataProvider? {
        if (config.enabled.not()) return null

        val bookWalkerMapper = BookWalkerMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher

        return BookWalkerMetadataProvider(
            client,
            bookWalkerMapper,
            similarityMatcher,
            config.seriesMetadata.thumbnail,
            config.bookMetadata.thumbnail,
            config.mediaType
        )
    }

    private fun createMangaDexMetadataProvider(
        config: ProviderConfig,
        client: MangaDexClient,
    ): MangaDexMetadataProvider? {
        if (config.enabled.not()) return null

        val mangaDexMetadataMapper = MangaDexMetadataMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val mangaDexSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return MangaDexMetadataProvider(
            client,
            mangaDexMetadataMapper,
            mangaDexSimilarityMatcher,
            config.seriesMetadata.thumbnail,
            config.bookMetadata.thumbnail
        )
    }

    private fun createBangumiMetadataProvider(
        config: ProviderConfig,
        client: BangumiClient,
    ): BangumiMetadataProvider? {
        if (config.enabled.not()) return null

        val bangumiMetadataMapper = BangumiMetadataMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val bangumiSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher
        return BangumiMetadataProvider(
            client,
            bangumiMetadataMapper,
            bangumiSimilarityMatcher,
            config.seriesMetadata.thumbnail,
            config.mediaType,
        )
    }

    private fun createComicVineMetadataProvider(
        config: ProviderConfig,
        client: ComicVineClient?,
    ): ComicVineMetadataProvider? {
        if (config.enabled.not()) return null
        if (client == null) throw ValidationException("Api key is not configured for ComicVine provider")

        val metadataMapper = ComicVineMetadataMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
        )
        val similarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { NameSimilarityMatcher.getInstance(it) } ?: nameSimilarityMatcher

        return ComicVineMetadataProvider(
            client,
            metadataMapper,
            similarityMatcher,
        )
    }

    class MetadataProviders(
        private val defaultProviders: MetadataProvidersContainer,
        private val libraryProviders: Map<String, MetadataProvidersContainer>,
    ) {
        fun defaultProvidersList() = defaultProviders.providers

        fun defaultProviders() = defaultProviders

        fun providers(libraryId: String): Collection<MetadataProvider> {
            return libraryProviders[libraryId]?.providers ?: defaultProviders.providers
        }

        fun provider(libraryId: String, provider: Provider) =
            libraryProviders[libraryId]?.provider(provider) ?: defaultProviders.provider(provider)
    }

    class MetadataProvidersContainer(
        private val mangaupdates: MangaUpdatesMetadataProvider?,
        private val mangaupdatesPriority: Int,

        private val mal: MalMetadataProvider?,
        private val malPriority: Int,

        private val nautiljon: NautiljonMetadataProvider?,
        private val nautiljonPriority: Int,

        private val anilist: AniListMetadataProvider?,
        private val anilistPriority: Int,

        private val yenPress: YenPressMetadataProvider?,
        private val yenPressPriority: Int,

        private val kodansha: KodanshaMetadataProvider?,
        private val kodanshaPriority: Int,

        private val viz: VizMetadataProvider?,
        private val vizPriority: Int,

        private val bookwalker: BookWalkerMetadataProvider?,
        private val bookwalkerPriority: Int,

        private val mangaDex: MangaDexMetadataProvider?,
        private val mangaDexPriority: Int,

        private val bangumi: BangumiMetadataProvider?,
        private val bangumiPriority: Int,

        private val comicVine: ComicVineMetadataProvider?,
        private val comicVinePriority: Int
    ) {

        val providers = listOfNotNull(
            mangaupdates?.let { it to mangaupdatesPriority },
            mal?.let { it to malPriority },
            nautiljon?.let { it to nautiljonPriority },
            anilist?.let { it to anilistPriority },
            yenPress?.let { it to yenPressPriority },
            kodansha?.let { it to kodanshaPriority },
            viz?.let { it to vizPriority },
            bookwalker?.let { it to bookwalkerPriority },
            mangaDex?.let { it to mangaDexPriority },
            bangumi?.let { it to bangumiPriority },
            comicVine?.let { it to comicVinePriority }
        )
            .sortedBy { (_, priority) -> priority }
            .toMap()
            .map { (provider, _) -> provider }

        fun provider(provider: Provider): MetadataProvider? {
            return when (provider) {
                Provider.MAL -> mal
                Provider.MANGA_UPDATES -> mangaupdates
                Provider.NAUTILJON -> nautiljon
                Provider.ANILIST -> anilist
                Provider.YEN_PRESS -> yenPress
                Provider.KODANSHA -> kodansha
                Provider.VIZ -> viz
                Provider.BOOK_WALKER -> bookwalker
                Provider.MANGADEX -> mangaDex
                Provider.BANGUMI -> bangumi
                Provider.COMIC_VINE -> comicVine
            }
        }
    }

    override fun close() {
        aniListClient.close()
    }
}

