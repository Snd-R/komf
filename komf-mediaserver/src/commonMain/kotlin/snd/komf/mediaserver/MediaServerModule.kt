package snd.komf.mediaserver

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import snd.komf.comicinfo.ComicInfoWriter
import snd.komf.ktor.komfUserAgent
import snd.komf.mediaserver.config.DatabaseConfig
import snd.komf.mediaserver.config.KavitaConfig
import snd.komf.mediaserver.config.KomgaConfig
import snd.komf.mediaserver.config.MetadataProcessingConfig
import snd.komf.mediaserver.config.MetadataUpdateConfig
import snd.komf.mediaserver.jobs.KomfJobTracker
import snd.komf.mediaserver.jobs.repository.KomfJobsRepository
import snd.komf.mediaserver.kavita.JvmJwtConsumer
import snd.komf.mediaserver.kavita.KavitaAuthClient
import snd.komf.mediaserver.kavita.KavitaClient
import snd.komf.mediaserver.kavita.KavitaEventHandler
import snd.komf.mediaserver.kavita.KavitaMediaServerClientAdapter
import snd.komf.mediaserver.kavita.KavitaTokenProvider
import snd.komf.mediaserver.komga.KomgaEventHandler
import snd.komf.mediaserver.komga.KomgaMediaServerClientAdapter
import snd.komf.mediaserver.match.repository.BookThumbnailsRepository
import snd.komf.mediaserver.match.repository.SeriesMatchRepository
import snd.komf.mediaserver.match.repository.SeriesThumbnailsRepository
import snd.komf.mediaserver.metadata.MetadataEventHandler
import snd.komf.mediaserver.metadata.MetadataMapper
import snd.komf.mediaserver.metadata.MetadataMerger
import snd.komf.mediaserver.metadata.MetadataPostProcessor
import snd.komf.mediaserver.metadata.MetadataService
import snd.komf.mediaserver.metadata.MetadataUpdater
import snd.komf.mediaserver.model.MediaServer
import snd.komf.notifications.apprise.AppriseCliService
import snd.komf.notifications.discord.DiscordWebhookService
import snd.komf.providers.ProvidersModule
import snd.komga.client.KomgaClientFactory
import java.nio.file.Path
import kotlin.time.Clock

class MediaServerModule(
    komgaConfig: KomgaConfig,
    kavitaConfig: KavitaConfig,
    databaseConfig: DatabaseConfig,
    jsonBase: Json,
    ktorBaseClient: HttpClient,
    appriseService: AppriseCliService,
    discordWebhookService: DiscordWebhookService,
    private val metadataProviders: ProvidersModule.MetadataProviders,
) {
    private val mediaServerDatabase = createDatabase(Path.of(databaseConfig.file))
    val jobRepository = KomfJobsRepository(mediaServerDatabase)
    val jobTracker = KomfJobTracker(jobRepository)

    val komgaClient: KomgaMediaServerClientAdapter
    val komgaMetadataServiceProvider: MetadataServiceProvider
    private val komgaBookThumbnailRepository: BookThumbnailsRepository
    private val komgaSerThumbnailsRepository: SeriesThumbnailsRepository
    val komgaSeriesMatchRepository: SeriesMatchRepository
    private val komgaMetadataEventHandler: MetadataEventHandler
    private val komgaNotificationsHandler: NotificationsEventHandler?
    private val komgaEventHandler: KomgaEventHandler

    val kavitaMediaServerClient: KavitaMediaServerClientAdapter
    val kavitaMetadataServiceProvider: MetadataServiceProvider
    private val kavitaBookThumbnailRepository: BookThumbnailsRepository
    private val kavitaSerThumbnailsRepository: SeriesThumbnailsRepository
    val kavitaSeriesMatchRepository: SeriesMatchRepository
    private val kavitaKtorBase: HttpClient
    private val kavitaAuthClient: KavitaAuthClient
    private val kavitaTokenProvider: KavitaTokenProvider
    private val kavitaKtorClient: HttpClient
    private val kavitaClient: KavitaClient
    private val kavitaMetadataEventHandler: MetadataEventHandler
    private val kavitaNotificationsHandler: NotificationsEventHandler?
    private val kavitaEventHandler: KavitaEventHandler

    init {
        val komgaClientFactory = KomgaClientFactory.Builder()
            .ktor(ktorBaseClient)
            .cookieStorage(AcceptAllCookiesStorage())
            .username(komgaConfig.komgaUser)
            .password(komgaConfig.komgaPassword)
            .baseUrlBuilder { URLBuilder(komgaConfig.baseUri).appendPathSegments("/") }
            .useragent(komfUserAgent)
            .build()
        komgaClient = KomgaMediaServerClientAdapter(
            komgaClientFactory.bookClient(),
            komgaClientFactory.seriesClient(),
            komgaClientFactory.libraryClient(),
            komgaConfig.thumbnailSizeLimit
        )
        komgaBookThumbnailRepository = BookThumbnailsRepository(
            mediaServerDatabase,
            MediaServer.KOMGA
        )
        komgaSerThumbnailsRepository = SeriesThumbnailsRepository(
            mediaServerDatabase,
            MediaServer.KOMGA
        )
        komgaSeriesMatchRepository = SeriesMatchRepository(
            mediaServerDatabase,
            MediaServer.KOMGA
        )
        komgaMetadataServiceProvider = createMetadataServiceProvider(
            config = komgaConfig.metadataUpdate,
            mediaServerClient = komgaClient,
            seriesThumbnailsRepository = komgaSerThumbnailsRepository,
            bookThumbnailsRepository = komgaBookThumbnailRepository,
            seriesMatchRepository = komgaSeriesMatchRepository,
        )

        komgaMetadataEventHandler = MetadataEventHandler(
            metadataServiceProvider = komgaMetadataServiceProvider,
            bookThumbnailsRepository = komgaBookThumbnailRepository,
            seriesThumbnailsRepository = komgaSerThumbnailsRepository,
            seriesMatchRepository = komgaSeriesMatchRepository,
            jobTracker = jobTracker,
            libraryFilter = {
                val libraries = komgaConfig.eventListener.metadataLibraryFilter
                if (libraries.isEmpty()) true
                else libraries.contains(it)
            },
            seriesFilter = { seriesId -> komgaConfig.eventListener.metadataSeriesExcludeFilter.none { seriesId == it } },
        )
        komgaNotificationsHandler = NotificationsEventHandler(
            mediaServerClient = komgaClient,
            appriseService = appriseService,
            discordWebhookService = discordWebhookService,
            libraryFilter = {
                val libraries = komgaConfig.eventListener.notificationsLibraryFilter
                if (libraries.isEmpty()) true
                else libraries.contains(it)
            },
            mediaServer = MediaServer.KOMGA
        )

        komgaEventHandler = KomgaEventHandler(
            eventSourceFactory = { komgaClientFactory.sseSession() },
            eventListeners = listOfNotNull(komgaMetadataEventHandler, komgaNotificationsHandler),
        )


        kavitaBookThumbnailRepository = BookThumbnailsRepository(
            mediaServerDatabase,
            MediaServer.KAVITA
        )
        kavitaSerThumbnailsRepository = SeriesThumbnailsRepository(
            mediaServerDatabase,
            MediaServer.KAVITA
        )
        kavitaSeriesMatchRepository = SeriesMatchRepository(
            mediaServerDatabase,
            MediaServer.KAVITA
        )
        kavitaKtorBase = ktorBaseClient.config {
            defaultRequest {
                url {
                    this.takeFrom(io.ktor.http.URLBuilder(kavitaConfig.baseUri).appendPathSegments("/"))
                }
            }
            install(ContentNegotiation) { json(jsonBase) }
        }
        kavitaAuthClient = KavitaAuthClient(kavitaKtorBase)
        kavitaTokenProvider = KavitaTokenProvider(
            kavitaClient = kavitaAuthClient,
            apiKey = kavitaConfig.apiKey,
            jwtConsumer = JvmJwtConsumer(),
            clock = Clock.System
        )
        kavitaKtorClient = kavitaKtorBase.config {
            install(Auth) {
                bearer { loadTokens { BearerTokens(kavitaTokenProvider.getToken(), null) } }
            }
        }
        kavitaClient = KavitaClient(kavitaKtorClient, jsonBase, kavitaConfig.apiKey)
        kavitaMediaServerClient = KavitaMediaServerClientAdapter(kavitaClient)
        kavitaMetadataServiceProvider = createMetadataServiceProvider(
            config = kavitaConfig.metadataUpdate,
            mediaServerClient = kavitaMediaServerClient,
            seriesThumbnailsRepository = komgaSerThumbnailsRepository,
            bookThumbnailsRepository = komgaBookThumbnailRepository,
            seriesMatchRepository = komgaSeriesMatchRepository,
        )
        kavitaMetadataEventHandler = MetadataEventHandler(
            metadataServiceProvider = kavitaMetadataServiceProvider,
            bookThumbnailsRepository = kavitaBookThumbnailRepository,
            seriesThumbnailsRepository = kavitaSerThumbnailsRepository,
            seriesMatchRepository = kavitaSeriesMatchRepository,
            jobTracker = jobTracker,
            libraryFilter = {
                val libraries = kavitaConfig.eventListener.metadataLibraryFilter
                if (libraries.isEmpty()) true
                else libraries.contains(it)
            },
            seriesFilter = { seriesId -> kavitaConfig.eventListener.metadataSeriesExcludeFilter.none { seriesId == it } },
        )
        kavitaNotificationsHandler = NotificationsEventHandler(
            mediaServerClient = kavitaMediaServerClient,
            appriseService = appriseService,
            discordWebhookService = discordWebhookService,
            libraryFilter = {
                val libraries = kavitaConfig.eventListener.notificationsLibraryFilter
                if (libraries.isEmpty()) true
                else libraries.contains(it)
            },
            mediaServer = MediaServer.KAVITA
        )

        kavitaEventHandler = KavitaEventHandler(
            baseUrl = io.ktor.http.URLBuilder(kavitaConfig.baseUri),
            kavitaClient = kavitaClient,
            tokenProvider = kavitaTokenProvider,
            clock = Clock.System,
            eventListeners = listOfNotNull(kavitaMetadataEventHandler, kavitaNotificationsHandler),
        )
        if (kavitaConfig.eventListener.enabled) {
            kavitaEventHandler.start()
        }
        if (komgaConfig.eventListener.enabled) {
            komgaEventHandler.start()
        }
    }

    fun close() {
        komgaEventHandler.stop()
        kavitaEventHandler.stop()
    }

    private fun createMetadataServiceProvider(
        config: MetadataUpdateConfig,
        mediaServerClient: MediaServerClient,
        seriesThumbnailsRepository: SeriesThumbnailsRepository,
        bookThumbnailsRepository: BookThumbnailsRepository,
        seriesMatchRepository: SeriesMatchRepository,
    ): MetadataServiceProvider {
        val defaultUpdaterService = createMetadataUpdateService(
            config = config.default,
            mediaServerClient = mediaServerClient,
            seriesThumbnailsRepository = seriesThumbnailsRepository,
            bookThumbnailsRepository = bookThumbnailsRepository
        )

        val libraryUpdaterServices = config.library
            .map { (libraryId, config) ->
                libraryId to createMetadataUpdateService(
                    config = config,
                    mediaServerClient = mediaServerClient,
                    seriesThumbnailsRepository = seriesThumbnailsRepository,
                    bookThumbnailsRepository = bookThumbnailsRepository
                )
            }
            .toMap()

        val defaultMetadataService = createMetadataService(
            config = config.default,
            mediaServerClient = mediaServerClient,
            seriesMatchRepository = seriesMatchRepository,
            metadataUpdateService = defaultUpdaterService
        )
        val libraryMetadataServices = config.library
            .map { (libraryId, config) ->
                libraryId to createMetadataService(
                    config = config,
                    mediaServerClient = mediaServerClient,
                    seriesMatchRepository = seriesMatchRepository,
                    metadataUpdateService = libraryUpdaterServices[libraryId] ?: defaultUpdaterService
                )
            }
            .toMap()

        return MetadataServiceProvider(
            defaultMetadataService = defaultMetadataService,
            libraryMetadataServices = libraryMetadataServices,
            defaultUpdateService = defaultUpdaterService,
            libraryUpdaterServices = libraryUpdaterServices
        )
    }

    private fun createMetadataService(
        config: MetadataProcessingConfig,
        metadataUpdateService: MetadataUpdater,
        mediaServerClient: MediaServerClient,
        seriesMatchRepository: SeriesMatchRepository,
    ): MetadataService {
        return MetadataService(
            mediaServerClient = mediaServerClient,
            metadataProviders = metadataProviders,
            aggregateMetadata = config.aggregate,
            metadataUpdateService = metadataUpdateService,
            seriesMatchRepository = seriesMatchRepository,
            metadataMerger = MetadataMerger(mergeTags = config.mergeTags, mergeGenres = config.mergeGenres),
            libraryType = config.libraryType,
            jobTracker = jobTracker,
        )
    }

    private fun createMetadataUpdateService(
        config: MetadataProcessingConfig,
        mediaServerClient: MediaServerClient,
        seriesThumbnailsRepository: SeriesThumbnailsRepository,
        bookThumbnailsRepository: BookThumbnailsRepository,
    ): MetadataUpdater {
        val postProcessor = MetadataPostProcessor(
            libraryType = config.libraryType,
            seriesTitle = config.postProcessing.seriesTitle,
            seriesTitleLanguage = config.postProcessing.seriesTitleLanguage,
            alternativeSeriesTitles = config.postProcessing.alternativeSeriesTitles,
            alternativeSeriesTitleLanguages = config.postProcessing.alternativeSeriesTitleLanguages,
            orderBooks = config.postProcessing.orderBooks,
            readingDirectionValue = config.postProcessing.readingDirectionValue,
            languageValue = config.postProcessing.languageValue,
            fallbackToAltTitle = config.postProcessing.fallbackToAltTitle,

            scoreTagName = config.postProcessing.scoreTagName,
            originalPublisherTagName = config.postProcessing.originalPublisherTagName,
            publisherTagNames = config.postProcessing.publisherTagNames
        )

        return MetadataUpdater(
            mediaServerClient = mediaServerClient,
            seriesThumbnailsRepository = seriesThumbnailsRepository,
            bookThumbnailsRepository = bookThumbnailsRepository,
            metadataUpdateMapper = MetadataMapper(),
            postProcessor = postProcessor,
            comicInfoWriter = ComicInfoWriter.Companion.getInstance(config.overrideComicInfo),

            updateModes = config.updateModes.toSet(),
            uploadBookCovers = config.bookCovers,
            uploadSeriesCovers = config.seriesCovers,
            overrideExistingCovers = config.overrideExistingCovers,
            lockCovers = config.lockCovers,
        )
    }

    fun createDatabase(file: Path): Database {
        val config = SQLiteConfig().apply {
            enforceForeignKeys(true)
            busyTimeout = 5_000
        }
        val datasource = HikariDataSource(
            HikariConfig().apply {
                dataSource = SQLiteDataSource(config).apply { url = "jdbc:sqlite:${file}" }
                poolName = "app db pool"
                maximumPoolSize = 1
            }
        )
        Flyway(
            Flyway.configure(MediaServerModule::class.java.classLoader)
                .loggers("slf4j")
                .dataSource(datasource)
                .locations("db/migration")
                .baselineOnMigrate(true)
        ).migrate()

        return Database.connect(
            datasource = datasource,
        )
    }
}
