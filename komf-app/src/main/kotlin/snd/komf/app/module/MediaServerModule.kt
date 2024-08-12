package snd.komf.app.module

import io.ktor.client.*
import io.ktor.client.plugins.cookies.*
import snd.komf.app.config.KomgaConfig
import snd.komf.app.config.MetadataProcessingConfig
import snd.komf.app.config.MetadataUpdateConfig
import snd.komf.comicinfo.JvmComicInfoWriter
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.MetadataServiceProvider
import snd.komf.mediaserver.jobs.KomfJobTracker
import snd.komf.mediaserver.jobs.KomfJobsRepository
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
import snd.komf.mediaserver.model.MediaServer.KOMGA
import snd.komf.mediaserver.repository.Database
import snd.komf.notifications.discord.DiscordWebhookService
import snd.komf.notifications.discord.NotificationsEventHandler
import snd.komf.providers.ProviderFactory.MetadataProviders
import snd.komga.client.KomgaClientFactory

class MediaServerModule(
    komgaConfig: KomgaConfig,
    ktorBaseClient: HttpClient,
    mediaServerDatabase: Database,
    discordWebhookService: DiscordWebhookService?,
    private val metadataProviders: MetadataProviders,
) {

    private val komgaClientFactory = KomgaClientFactory.Builder()
        .ktor(ktorBaseClient)
        .cookieStorage(AcceptAllCookiesStorage())
        .username(komgaConfig.komgaUser)
        .password(komgaConfig.komgaPassword)
        .baseUrl { komgaConfig.baseUri }
        .build()

    val komgaClient = KomgaMediaServerClientAdapter(
        komgaClientFactory.bookClient(),
        komgaClientFactory.seriesClient(),
        komgaClientFactory.libraryClient(),
        komgaConfig.thumbnailSizeLimit
    )

     val jobRepository = KomfJobsRepository(mediaServerDatabase.komfJobRecordQueries)
    private val komgaBookThumbnailRepository = BookThumbnailsRepository(mediaServerDatabase.bookThumbnailQueries, KOMGA)
    private val komgaSerThumbnailsRepository =
        SeriesThumbnailsRepository(mediaServerDatabase.seriesThumbnailQueries, KOMGA)
    private val komgaSeriesMatchRepository = SeriesMatchRepository(mediaServerDatabase.seriesMatchQueries, KOMGA)
     val jobTracker = KomfJobTracker(jobRepository)

    val komgaMetadataServiceProvider: MetadataServiceProvider = createMetadataServiceProvider(
        config = komgaConfig.metadataUpdate,
        mediaServerClient = komgaClient,
        seriesThumbnailsRepository = komgaSerThumbnailsRepository,
        bookThumbnailsRepository = komgaBookThumbnailRepository,
        seriesMatchRepository = komgaSeriesMatchRepository,
    )

    private val komgaMetadataEventHandler = MetadataEventHandler(
        metadataServiceProvider = komgaMetadataServiceProvider,
        bookThumbnailsRepository = komgaBookThumbnailRepository,
        seriesThumbnailsRepository = komgaSerThumbnailsRepository,
        seriesMatchRepository = komgaSeriesMatchRepository,
        libraryFilter = { komgaConfig.eventListener.metadataLibraryFilter.contains(it) },
        seriesFilter = { seriesId -> komgaConfig.eventListener.metadataSeriesExcludeFilter.none { seriesId == it } },
    )
    private val komgaNotificationsHandler = discordWebhookService?.let {
        NotificationsEventHandler(
            mediaServerClient = komgaClient,
            discordWebhookService = discordWebhookService,
            libraryFilter = { komgaConfig.eventListener.notificationsLibraryFilter.contains(it) },
            mediaServer = KOMGA
        )
    }

    private val komgaEventListener = KomgaEventHandler(
        eventSourceFactory = { komgaClientFactory.sseSession() },
        eventListeners = listOfNotNull(komgaMetadataEventHandler, komgaNotificationsHandler),
    )


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
            config.libraryType,
            config.postProcessing.seriesTitle,
            config.postProcessing.seriesTitleLanguage,
            config.postProcessing.alternativeSeriesTitles,
            config.postProcessing.alternativeSeriesTitleLanguages,
            config.postProcessing.orderBooks,
            config.postProcessing.scoreTag,
            config.postProcessing.readingDirectionValue,
            config.postProcessing.languageValue,
            config.postProcessing.fallbackToAltTitle,
        )

        return MetadataUpdater(
            mediaServerClient = mediaServerClient,
            seriesThumbnailsRepository = seriesThumbnailsRepository,
            bookThumbnailsRepository = bookThumbnailsRepository,
            metadataUpdateMapper = MetadataMapper(),
            postProcessor = postProcessor,
            comicInfoWriter = JvmComicInfoWriter.getInstance(config.overrideComicInfo),

            updateModes = config.updateModes.toSet(),
            uploadBookCovers = config.bookCovers,
            uploadSeriesCovers = config.seriesCovers,
            overrideExistingCovers = config.overrideExistingCovers
        )
    }

    fun close() {
        komgaEventListener.stop()
    }
}