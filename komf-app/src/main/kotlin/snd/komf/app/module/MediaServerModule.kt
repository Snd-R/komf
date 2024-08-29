package snd.komf.app.module

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import snd.komf.app.config.KavitaConfig
import snd.komf.app.config.KomgaConfig
import snd.komf.app.config.MetadataProcessingConfig
import snd.komf.app.config.MetadataUpdateConfig
import snd.komf.comicinfo.ComicInfoWriter
import snd.komf.ktor.komfUserAgent
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.MetadataServiceProvider
import snd.komf.mediaserver.jobs.KomfJobTracker
import snd.komf.mediaserver.jobs.KomfJobsRepository
import snd.komf.mediaserver.kavita.JvmJwtConsumer
import snd.komf.mediaserver.kavita.KavitaAuthClient
import snd.komf.mediaserver.kavita.KavitaClient
import snd.komf.mediaserver.kavita.KavitaEventHandler
import snd.komf.mediaserver.kavita.KavitaMediaServerClientAdapter
import snd.komf.mediaserver.kavita.KavitaTokenProvider
import snd.komf.mediaserver.komga.KomgaEventHandler
import snd.komf.mediaserver.komga.KomgaMediaServerClientAdapter
import snd.komf.mediaserver.metadata.MetadataEventHandler
import snd.komf.mediaserver.metadata.MetadataMapper
import snd.komf.mediaserver.metadata.MetadataMerger
import snd.komf.mediaserver.metadata.MetadataPostProcessor
import snd.komf.mediaserver.metadata.MetadataService
import snd.komf.mediaserver.metadata.MetadataUpdater
import snd.komf.mediaserver.metadata.repository.BookThumbnailsRepository
import snd.komf.mediaserver.metadata.repository.SeriesMatchRepository
import snd.komf.mediaserver.metadata.repository.SeriesThumbnailsRepository
import snd.komf.mediaserver.model.MediaServer.KAVITA
import snd.komf.mediaserver.model.MediaServer.KOMGA
import snd.komf.mediaserver.repository.Database
import snd.komf.notifications.NotificationsEventHandler
import snd.komf.notifications.apprise.AppriseCliService
import snd.komf.notifications.discord.DiscordWebhookService
import snd.komf.providers.ProviderFactory.MetadataProviders
import snd.komga.client.KomgaClientFactory

class MediaServerModule(
    komgaConfig: KomgaConfig,
    kavitaConfig: KavitaConfig,
    jsonBase: Json,
    ktorBaseClient: HttpClient,
    mediaServerDatabase: Database,
    appriseService: AppriseCliService,
    discordWebhookService: DiscordWebhookService,
    private val jobTracker: KomfJobTracker,
    private val metadataProviders: MetadataProviders,
) {

    val komgaClient: KomgaMediaServerClientAdapter
    val komgaMetadataServiceProvider: MetadataServiceProvider
    private val komgaBookThumbnailRepository: BookThumbnailsRepository
    private val komgaSerThumbnailsRepository: SeriesThumbnailsRepository
    private val komgaSeriesMatchRepository: SeriesMatchRepository
    private val komgaMetadataEventHandler: MetadataEventHandler
    private val komgaNotificationsHandler: NotificationsEventHandler?
    private val komgaEventHandler: KomgaEventHandler

    val kavitaMediaServerClient: KavitaMediaServerClientAdapter
    val kavitaMetadataServiceProvider: MetadataServiceProvider
    private val kavitaBookThumbnailRepository: BookThumbnailsRepository
    private val kavitaSerThumbnailsRepository: SeriesThumbnailsRepository
    private val kavitaSeriesMatchRepository: SeriesMatchRepository
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
            .baseUrl { komgaConfig.baseUri }
            .useragent(komfUserAgent)
            .build()
        komgaClient = KomgaMediaServerClientAdapter(
            komgaClientFactory.bookClient(),
            komgaClientFactory.seriesClient(),
            komgaClientFactory.libraryClient(),
            komgaConfig.thumbnailSizeLimit
        )
        komgaBookThumbnailRepository = BookThumbnailsRepository(
            mediaServerDatabase.bookThumbnailQueries,
            KOMGA
        )
        komgaSerThumbnailsRepository = SeriesThumbnailsRepository(
            mediaServerDatabase.seriesThumbnailQueries,
            KOMGA
        )
        komgaSeriesMatchRepository = SeriesMatchRepository(
            mediaServerDatabase.seriesMatchQueries,
            KOMGA
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
            libraryFilter = { komgaConfig.eventListener.metadataLibraryFilter.contains(it) },
            seriesFilter = { seriesId -> komgaConfig.eventListener.metadataSeriesExcludeFilter.none { seriesId == it } },
        )
        komgaNotificationsHandler = NotificationsEventHandler(
            mediaServerClient = komgaClient,
            appriseService = appriseService,
            discordWebhookService = discordWebhookService,
            libraryFilter = { komgaConfig.eventListener.notificationsLibraryFilter.contains(it) },
            mediaServer = KOMGA
        )

        komgaEventHandler = KomgaEventHandler(
            eventSourceFactory = { komgaClientFactory.sseSession() },
            eventListeners = listOfNotNull(komgaMetadataEventHandler, komgaNotificationsHandler),
        )


        kavitaBookThumbnailRepository = BookThumbnailsRepository(
            mediaServerDatabase.bookThumbnailQueries,
            KAVITA
        )
        kavitaSerThumbnailsRepository = SeriesThumbnailsRepository(
            mediaServerDatabase.seriesThumbnailQueries,
            KAVITA
        )
        kavitaSeriesMatchRepository = SeriesMatchRepository(
            mediaServerDatabase.seriesMatchQueries,
            KAVITA
        )
        kavitaKtorBase = ktorBaseClient.config {
            defaultRequest { url(kavitaConfig.baseUri) }
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
            libraryFilter = { kavitaConfig.eventListener.metadataLibraryFilter.contains(it) },
            seriesFilter = { seriesId -> kavitaConfig.eventListener.metadataSeriesExcludeFilter.none { seriesId == it } },
        )
        kavitaNotificationsHandler = NotificationsEventHandler(
            mediaServerClient = kavitaMediaServerClient,
            appriseService = appriseService,
            discordWebhookService = discordWebhookService,
            libraryFilter = { kavitaConfig.eventListener.notificationsLibraryFilter.contains(it) },
            mediaServer = KAVITA
        )

        kavitaEventHandler = KavitaEventHandler(
            baseUrl = kavitaConfig.baseUri,
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

            scoreTag = config.postProcessing.scoreTag,
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
            comicInfoWriter = ComicInfoWriter.getInstance(config.overrideComicInfo),

            updateModes = config.updateModes.toSet(),
            uploadBookCovers = config.bookCovers,
            uploadSeriesCovers = config.seriesCovers,
            overrideExistingCovers = config.overrideExistingCovers
        )
    }
}
