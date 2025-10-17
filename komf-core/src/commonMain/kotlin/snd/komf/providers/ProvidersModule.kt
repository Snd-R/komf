package snd.komf.providers

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestRetryConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.ConstantCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode.Companion.TooManyRequests
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.exposed.v1.jdbc.Database
import snd.komf.ktor.HttpRequestRateLimiter
import snd.komf.ktor.intervalLimiter
import snd.komf.ktor.rateLimiter
import snd.komf.providers.anilist.AniListClient
import snd.komf.providers.anilist.AniListMetadataMapper
import snd.komf.providers.anilist.AniListMetadataProvider
import snd.komf.providers.bangumi.BangumiClient
import snd.komf.providers.bangumi.BangumiMetadataMapper
import snd.komf.providers.bangumi.BangumiMetadataProvider
import snd.komf.providers.bookwalker.BookWalkerClient
import snd.komf.providers.bookwalker.BookWalkerMapper
import snd.komf.providers.bookwalker.BookWalkerMetadataProvider
import snd.komf.providers.comicvine.ComicVineClient
import snd.komf.providers.comicvine.ComicVineMetadataMapper
import snd.komf.providers.comicvine.ComicVineMetadataProvider
import snd.komf.providers.comicvine.ComicVineRateLimiter
import snd.komf.providers.hentag.HentagClient
import snd.komf.providers.hentag.HentagMetadataMapper
import snd.komf.providers.hentag.HentagMetadataProvider
import snd.komf.providers.kodansha.KodanshaClient
import snd.komf.providers.kodansha.KodanshaMetadataMapper
import snd.komf.providers.kodansha.KodanshaMetadataProvider
import snd.komf.providers.mal.MalClient
import snd.komf.providers.mal.MalMetadataMapper
import snd.komf.providers.mal.MalMetadataProvider
import snd.komf.providers.mangabaka.MangaBakaDataSource
import snd.komf.providers.mangabaka.MangaBakaMetadataMapper
import snd.komf.providers.mangabaka.MangaBakaMetadataProvider
import snd.komf.providers.mangabaka.api.MangaBakaApiClient
import snd.komf.providers.mangabaka.db.MangaBakaDbDataSource
import snd.komf.providers.mangadex.MangaDexClient
import snd.komf.providers.mangadex.MangaDexMetadataMapper
import snd.komf.providers.mangadex.MangaDexMetadataProvider
import snd.komf.providers.mangadex.model.MangaDexArtist
import snd.komf.providers.mangadex.model.MangaDexAuthor
import snd.komf.providers.mangadex.model.MangaDexCoverArt
import snd.komf.providers.mangadex.model.MangaDexRelationship
import snd.komf.providers.mangadex.model.MangaDexUnknownRelationship
import snd.komf.providers.mangaupdates.MangaUpdatesClient
import snd.komf.providers.mangaupdates.MangaUpdatesMetadataMapper
import snd.komf.providers.mangaupdates.MangaUpdatesMetadataProvider
import snd.komf.providers.nautiljon.NautiljonClient
import snd.komf.providers.nautiljon.NautiljonMetadataProvider
import snd.komf.providers.nautiljon.NautiljonSeriesMetadataMapper
import snd.komf.providers.viz.VizClient
import snd.komf.providers.viz.VizMetadataMapper
import snd.komf.providers.viz.VizMetadataProvider
import snd.komf.providers.webtoons.WebtoonsClient
import snd.komf.providers.webtoons.WebtoonsMetadataMapper
import snd.komf.providers.webtoons.WebtoonsMetadataProvider
import snd.komf.providers.yenpress.YenPressClient
import snd.komf.providers.yenpress.YenPressMetadataMapper
import snd.komf.providers.yenpress.YenPressMetadataProvider
import snd.komf.util.NameSimilarityMatcher
import snd.komf.util.NameSimilarityMatcher.Companion.nameSimilarityMatcher
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

class ProvidersModule(
    private val config: MetadataProvidersConfig,
    baseHttpClient: HttpClient,
    mangaBakaDatabase: Database?,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false

        serializersModule = SerializersModule {
            polymorphic(MangaDexRelationship::class) {
                subclass(MangaDexAuthor::class)
                subclass(MangaDexArtist::class)
                subclass(MangaDexCoverArt::class)
                defaultDeserializer { MangaDexUnknownRelationship.serializer() }
            }
        }
    }

    fun getMetadataProviders(): MetadataProviders {
        val defaultNameMatcher = nameSimilarityMatcher(config.nameMatchingMode)
        val defaultProviders = createMetadataProviders(
            config = config.defaultProviders,
            defaultNameMatcher = defaultNameMatcher,
            malClientId = config.malClientId,
            comicVineClientId = config.comicVineApiKey,
            comicVineSearchLimit = config.comicVineSearchLimit,
            comicVineIssueName = config.comicVineIssueName,
            comicVineIdFormat = config.comicVineIdFormat,
            bangumiToken = config.bangumiToken,
        )
        val libraryProviders = config.libraryProviders
            .map { (libraryId, libraryConfig) ->
                libraryId to createMetadataProviders(
                    config = libraryConfig,
                    defaultNameMatcher = defaultNameMatcher,
                    malClientId = config.malClientId,
                    comicVineClientId = config.comicVineApiKey,
                    comicVineSearchLimit = config.comicVineSearchLimit,
                    comicVineIssueName = config.comicVineIssueName,
                    comicVineIdFormat = config.comicVineIdFormat,
                    bangumiToken = config.bangumiToken,
                )
            }
            .toMap()

        return MetadataProviders(defaultProviders, libraryProviders)
    }

    private val baseHttpClientJson = baseHttpClient.config {
        install(ContentNegotiation) { json(json) }
    }

    private val comicVineRateLimiter = ComicVineRateLimiter()
    private val malRateLimiter = rateLimiter(eventsPerInterval = 10, interval = 10.seconds)
    private val bangumiRateLimiter = intervalLimiter(eventsPerInterval = 10, interval = 7.seconds)

    private fun HttpRequestRetryConfig.defaultRetry() {
        retryIf(3) { _, response ->
            when (response.status.value) {
                TooManyRequests.value -> true
                420 -> true // ComicVine returns 420 response code
                in 500..599 -> true
                else -> false
            }
        }
        exponentialDelay(baseDelayMs = 2000, respectRetryAfterHeader = true)
    }

    private val mangaUpdatesClient = MangaUpdatesClient(
        baseHttpClientJson.config {
            install(HttpRequestRateLimiter) {
                interval = 10.seconds
                eventsPerInterval = 15
                allowBurst = true
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
        }
    )

    private val nautiljonClient = NautiljonClient(
        baseHttpClient.config {
            install(HttpRequestRateLimiter) {
                interval = 10.seconds
                eventsPerInterval = 10
                allowBurst = false
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
        }
    )

    private val aniListClient = AniListClient(
        baseHttpClientJson.config {
            install(HttpRequestRateLimiter) {
                interval = 10.seconds
                eventsPerInterval = 15
                allowBurst = true
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
        }
    )
    private val yenPressClient = YenPressClient(
        baseHttpClientJson.config {
            install(HttpRequestRateLimiter) {
                interval = 10.seconds
                eventsPerInterval = 10
                allowBurst = true
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
        }
    )
    private val kodanshaClient = KodanshaClient(
        baseHttpClientJson.config {
            install(HttpRequestRateLimiter) {
                interval = 10.seconds
                eventsPerInterval = 10
                allowBurst = true
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
        }
    )
    private val vizClient = VizClient(
        baseHttpClient.config {
            install(HttpRequestRateLimiter) {
                interval = 10.seconds
                eventsPerInterval = 5
                allowBurst = false
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
        }
    )
    private val bookWalkerClient = BookWalkerClient(
        ktor = baseHttpClient.config {
            install(HttpRequestRateLimiter) {
                interval = 10.seconds
                eventsPerInterval = 10
                allowBurst = true
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
        },
        json = json
    )
    private val mangaDexClient = MangaDexClient(
        baseHttpClientJson.config {
            install(HttpRequestRateLimiter) {
                interval = 10.seconds
                eventsPerInterval = 15
                allowBurst = true
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
        }
    )

    private val hentagClient = HentagClient(
        baseHttpClientJson.config {
            install(HttpRequestRateLimiter) {
                interval = 5.seconds
                eventsPerInterval = 1
                allowBurst = false
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
        }
    )

    private val mangaBakaClient = MangaBakaApiClient(
        baseHttpClientJson.config {
            install(HttpRequestRateLimiter) {
                interval = 1.seconds
                eventsPerInterval = 1
                allowBurst = false
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
        }
    )
    private val mangaBakaCoverFetchClient = baseHttpClientJson.config {
        install(HttpRequestRateLimiter) {
            interval = 1.seconds
            eventsPerInterval = 2
            allowBurst = false
        }
        install(HttpRequestRetry) {
            defaultRetry()
        }
    }


    private val mangaBakaDbDataSource = mangaBakaDatabase?.let { MangaBakaDbDataSource(it) }

    private val webtoonsClient = WebtoonsClient(
        baseHttpClientJson.config {
            install(HttpRequestRateLimiter) {
                interval = 1.seconds
                eventsPerInterval = 1
                allowBurst = false
            }
            install(HttpRequestRetry) {
                defaultRetry()
            }
            install(HttpCookies) {
                storage = ConstantCookiesStorage(
                    Cookie(
                        name = "ageGatePass",
                        value = "true",
                        domain = "www.webtoons.com",
                        path = "/"
                    ),
                    Cookie(
                        name = "locale",
                        value = "en", // Fixed to "en", maybe should be configurable?
                        domain = "www.webtoons.com",
                        path = "/"
                    ),
                    Cookie(
                        name = "needGDPR",
                        value = "false",
                        domain = "www.webtoons.com",
                        path = "/"
                    )
                )
            }
            install(ContentNegotiation) {
                json()
            }
        }
    )

    private fun createMetadataProviders(
        config: ProvidersConfig,
        defaultNameMatcher: NameSimilarityMatcher,
        malClientId: String?,
        comicVineClientId: String?,
        comicVineSearchLimit: Int?,
        comicVineIssueName: String?,
        comicVineIdFormat: String?,
        bangumiToken: String?,
    ): MetadataProvidersContainer {
        return MetadataProvidersContainer(
            mangaupdates = createMangaUpdatesMetadataProvider(
                config.mangaUpdates,
                mangaUpdatesClient,
                defaultNameMatcher
            ),
            mangaupdatesPriority = config.mangaUpdates.priority,
            mal = createMalMetadataProvider(
                config.mal,
                malClientId,
                defaultNameMatcher
            ),
            malPriority = config.mal.priority,
            nautiljon = createNautiljonMetadataProvider(
                config.nautiljon,
                nautiljonClient,
                defaultNameMatcher
            ),
            nautiljonPriority = config.nautiljon.priority,
            anilist = createAnilistMetadataProvider(
                config.aniList,
                aniListClient,
                defaultNameMatcher
            ),
            anilistPriority = config.aniList.priority,
            yenPress = createYenPressMetadataProvider(
                config.yenPress,
                yenPressClient,
                defaultNameMatcher
            ),
            yenPressPriority = config.yenPress.priority,
            kodansha = createKodanshaMetadataProvider(
                config.kodansha,
                kodanshaClient,
                defaultNameMatcher
            ),
            kodanshaPriority = config.kodansha.priority,
            viz = createVizMetadataProvider(
                config.viz,
                vizClient,
                defaultNameMatcher
            ),
            vizPriority = config.viz.priority,
            bookwalker = createBookWalkerMetadataProvider(
                config.bookWalker,
                bookWalkerClient,
                defaultNameMatcher
            ),
            bookwalkerPriority = config.bookWalker.priority,
            mangaDex = createMangaDexMetadataProvider(
                config.mangaDex,
                mangaDexClient,
                defaultNameMatcher
            ),
            mangaDexPriority = config.mangaDex.priority,
            bangumi = createBangumiMetadataProvider(
                config.bangumi,
                defaultNameMatcher,
                bangumiToken
            ),
            bangumiPriority = config.bangumi.priority,
            comicVine = createComicVineMetadataProvider(
                config = config.comicVine,
                apiKey = comicVineClientId,
                comicVineSearchLimit = comicVineSearchLimit,
                comicVineIssueName = comicVineIssueName,
                comicVineIdFormat = comicVineIdFormat,
                rateLimiter = comicVineRateLimiter,
                defaultNameMatcher = defaultNameMatcher,
            ),
            comicVinePriority = config.comicVine.priority,
            hentag = createHentagMetadataProvider(
                config.hentag,
                hentagClient,
                defaultNameMatcher
            ),
            hentagPriority = config.hentag.priority,
            mangaBaka = createMangaBakaMetadataProvider(
                config = config.mangaBaka,
                datasource = when (config.mangaBaka.mode) {
                    MangaBakaMode.API -> mangaBakaClient
                    MangaBakaMode.DATABASE -> mangaBakaDbDataSource
                },
                coverFetchClient = mangaBakaCoverFetchClient,
                defaultNameMatcher = defaultNameMatcher
            ),
            mangaBakaPriority = config.mangaBaka.priority,
            webtoons = createWebtoonsMetadataProvider(
                config = config.webtoons,
                client = webtoonsClient,
                defaultNameMatcher = defaultNameMatcher
            ),
            webtoonsPriority = config.webtoons.priority
        )
    }

    private fun createMalMetadataProvider(
        config: ProviderConfig,
        clientId: String?,
        defaultNameMatcher: NameSimilarityMatcher,
    ): MalMetadataProvider? {
        if (config.enabled.not()) return null
        requireNotNull(clientId) { "MyAnimeList clientId is not set" }

        val malClient = MalClient(
            baseHttpClientJson.config {
                install(HttpRequestRateLimiter) {
                    preconfigured = malRateLimiter
                }
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 3)
                    exponentialDelay(respectRetryAfterHeader = true)
                }
                defaultRequest {
                    header("X-MAL-CLIENT-ID", clientId)
                }
            }
        )

        val malMetadataMapper = MalMetadataMapper(
            metadataConfig = config.seriesMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val malSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher
        return MalMetadataProvider(
            malClient,
            malMetadataMapper,
            malSimilarityMatcher,
            config.seriesMetadata.thumbnail,
            config.mediaType,
        )
    }

    private fun createMangaUpdatesMetadataProvider(
        config: ProviderConfig,
        client: MangaUpdatesClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): MangaUpdatesMetadataProvider? {
        if (config.enabled.not()) return null

        val mangaUpdatesMetadataMapper = MangaUpdatesMetadataMapper(
            metadataConfig = config.seriesMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val mangaUpdatesSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher
        return MangaUpdatesMetadataProvider(
            client,
            mangaUpdatesMetadataMapper,
            mangaUpdatesSimilarityMatcher,
            config.seriesMetadata.thumbnail,
            config.mediaType
        )
    }

    private fun createNautiljonMetadataProvider(
        config: ProviderConfig,
        client: NautiljonClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): NautiljonMetadataProvider? {
        if (config.enabled.not()) return null
        val seriesMetadataMapper = NautiljonSeriesMetadataMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher
        return NautiljonMetadataProvider(
            client,
            seriesMetadataMapper,
            similarityMatcher,
            config.seriesMetadata.thumbnail,
            config.bookMetadata.thumbnail,
        )
    }

    private fun createAnilistMetadataProvider(
        config: AniListConfig,
        client: AniListClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): AniListMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = AniListMetadataMapper(
            metadataConfig = config.seriesMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
            tagsSizeLimit = config.tagsSizeLimit,
            tagsScoreThreshold = config.tagsScoreThreshold
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher
        return AniListMetadataProvider(
            client,
            metadataMapper,
            similarityMatcher,
            config.seriesMetadata.thumbnail,
            config.mediaType
        )
    }

    private fun createYenPressMetadataProvider(
        config: ProviderConfig,
        client: YenPressClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): YenPressMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = YenPressMetadataMapper(
            config.seriesMetadata,
            config.bookMetadata,
            config.authorRoles,
            config.artistRoles
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher
        return YenPressMetadataProvider(
            client,
            metadataMapper,
            similarityMatcher,
            config.mediaType,
            config.seriesMetadata.thumbnail,
            config.bookMetadata.thumbnail,
        )
    }

    private fun createKodanshaMetadataProvider(
        config: ProviderConfig,
        client: KodanshaClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): KodanshaMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = KodanshaMetadataMapper(config.seriesMetadata, config.bookMetadata)
        val similarityMatcher =
            config.nameMatchingMode?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher

        return KodanshaMetadataProvider(
            client,
            metadataMapper,
            similarityMatcher,
            config.seriesMetadata.thumbnail,
            config.bookMetadata.thumbnail,
        )
    }

    private fun createVizMetadataProvider(
        config: ProviderConfig,
        client: VizClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): VizMetadataProvider? {
        if (config.enabled.not()) return null

        val metadataMapper = VizMetadataMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher

        return VizMetadataProvider(
            client,
            metadataMapper,
            similarityMatcher,
            config.seriesMetadata.thumbnail,
            config.bookMetadata.thumbnail,
        )
    }

    private fun createBookWalkerMetadataProvider(
        config: ProviderConfig,
        client: BookWalkerClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): BookWalkerMetadataProvider? {
        if (config.enabled.not()) return null

        val bookWalkerMapper = BookWalkerMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val similarityMatcher = config.nameMatchingMode
            ?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher

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
        config: MangaDexConfig,
        client: MangaDexClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): MangaDexMetadataProvider? {
        if (config.enabled.not()) return null

        val mangaDexMetadataMapper = MangaDexMetadataMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
            coverLanguages = config.coverLanguages,
            linksFilter = config.links
        )

        val mangaDexSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher
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
        defaultNameMatcher: NameSimilarityMatcher,
        token: String?,
    ): BangumiMetadataProvider? {
        if (config.enabled.not()) return null
        val client = BangumiClient(
            baseHttpClientJson.config {
                install(HttpRequestRateLimiter) {
                    preconfigured = bangumiRateLimiter
                }
                install(HttpRequestRetry) {
                    defaultRetry()
                }

                if (!token.isNullOrBlank()) defaultRequest { bearerAuth(token) }
            },
        )
        val bangumiMetadataMapper = BangumiMetadataMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
        )
        val bangumiSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher
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
        apiKey: String?,
        comicVineSearchLimit: Int? = 10,
        comicVineIssueName: String?,
        comicVineIdFormat: String?,
        rateLimiter: ComicVineRateLimiter,
        defaultNameMatcher: NameSimilarityMatcher,
    ): ComicVineMetadataProvider? {
        if (config.enabled.not()) return null
        requireNotNull(apiKey) { "Api key is not configured for ComicVine provider" }

        val comicVineClient = ComicVineClient(
            ktor = baseHttpClientJson.config {
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 3)
                    exponentialDelay(respectRetryAfterHeader = true)
                }
            },
            apiKey = apiKey,
            comicVineSearchLimit = comicVineSearchLimit,
            rateLimiter = rateLimiter
        )
        val metadataMapper = ComicVineMetadataMapper(
            seriesMetadataConfig = config.seriesMetadata,
            bookMetadataConfig = config.bookMetadata,
            issueNameTemplate = comicVineIssueName,
        )
        val similarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher

        return ComicVineMetadataProvider(
            client = comicVineClient,
            mapper = metadataMapper,
            nameMatcher = similarityMatcher,
            fetchSeriesCovers = config.seriesMetadata.thumbnail,
            fetchBookCovers = config.bookMetadata.thumbnail,
            idFormat = comicVineIdFormat,
        )
    }

    private fun createHentagMetadataProvider(
        config: ProviderConfig,
        client: HentagClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): HentagMetadataProvider? {
        if (config.enabled.not()) return null

        val hentagMetadataMapper = HentagMetadataMapper(
            metadataConfig = config.seriesMetadata,
            authorRoles = config.authorRoles,
        )

        val hentagSimilarityMatcher: NameSimilarityMatcher =
            config.nameMatchingMode?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher
        return HentagMetadataProvider(
            client,
            hentagMetadataMapper,
            hentagSimilarityMatcher,
            config.seriesMetadata.thumbnail,
        )
    }


    private fun createMangaBakaMetadataProvider(
        config: MangaBakaConfig,
        datasource: MangaBakaDataSource?,
        coverFetchClient: HttpClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): MangaBakaMetadataProvider? {
        if (config.enabled.not()) return null
        if (datasource == null) {
            logger.warn { "Failed to find MangaBaka database. Disabling MangaBaka provider" }
            return null
        }

        return MangaBakaMetadataProvider(
            dataSource = datasource,
            metadataMapper = MangaBakaMetadataMapper(
                metadataConfig = config.seriesMetadata,
                authorRoles = config.authorRoles,
                artistRoles = config.artistRoles,
            ),
            nameMatcher = config.nameMatchingMode?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher,
            coverFetchClient = if (config.seriesMetadata.thumbnail) coverFetchClient else null,
            mediaType = config.mediaType
        )
    }

    private fun createWebtoonsMetadataProvider(
        config: ProviderConfig,
        client: WebtoonsClient,
        defaultNameMatcher: NameSimilarityMatcher,
    ): WebtoonsMetadataProvider? {
        if (config.enabled.not()) return null

        return WebtoonsMetadataProvider(
            client = client,
            metadataMapper = WebtoonsMetadataMapper(
                metadataConfig = config.seriesMetadata,
                authorRoles = config.authorRoles,
                artistRoles = config.artistRoles,
            ),
            nameMatcher = config.nameMatchingMode?.let { nameSimilarityMatcher(it) } ?: defaultNameMatcher,
            fetchSeriesCovers = config.seriesMetadata.thumbnail,
            fetchBookCovers = config.bookMetadata.thumbnail,
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

        fun provider(libraryId: String, provider: CoreProviders) =
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
        private val comicVinePriority: Int,

        private val hentag: HentagMetadataProvider?,
        private val hentagPriority: Int,

        private val mangaBaka: MangaBakaMetadataProvider?,
        private val mangaBakaPriority: Int,

        private val webtoons: WebtoonsMetadataProvider?,
        private val webtoonsPriority: Int,
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
            comicVine?.let { it to comicVinePriority },
            hentag?.let { it to hentagPriority },
            mangaBaka?.let { it to mangaBakaPriority },
            webtoons?.let { it to webtoonsPriority }
        )
            .sortedBy { (_, priority) -> priority }
            .toMap()
            .map { (provider, _) -> provider }

        fun provider(provider: CoreProviders): MetadataProvider? {
            return when (provider) {
                CoreProviders.MAL -> mal
                CoreProviders.MANGA_UPDATES -> mangaupdates
                CoreProviders.NAUTILJON -> nautiljon
                CoreProviders.ANILIST -> anilist
                CoreProviders.YEN_PRESS -> yenPress
                CoreProviders.KODANSHA -> kodansha
                CoreProviders.VIZ -> viz
                CoreProviders.BOOK_WALKER -> bookwalker
                CoreProviders.MANGADEX -> mangaDex
                CoreProviders.BANGUMI -> bangumi
                CoreProviders.COMIC_VINE -> comicVine
                CoreProviders.HENTAG -> hentag
                CoreProviders.MANGA_BAKA -> mangaBaka
                CoreProviders.WEBTOONS -> webtoons
            }
        }
    }


}