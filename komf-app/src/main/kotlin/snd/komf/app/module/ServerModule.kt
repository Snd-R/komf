package snd.komf.app.module

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import snd.komf.app.AppContext
import snd.komf.app.api.ConfigRoutes
import snd.komf.app.api.JobRoutes
import snd.komf.app.api.MetadataRoutes
import snd.komf.app.api.NotificationRoutes
import snd.komf.app.api.deprecated.DeprecatedConfigRoutes
import snd.komf.app.api.deprecated.DeprecatedConfigUpdateMapper
import snd.komf.app.api.deprecated.DeprecatedMetadataRoutes
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.MetadataServiceProvider
import snd.komf.mediaserver.jobs.KomfJobTracker
import snd.komf.mediaserver.jobs.KomfJobsRepository
import snd.komf.mediaserver.model.MediaServer.KAVITA
import snd.komf.mediaserver.model.MediaServer.KOMGA
import snd.komf.notifications.discord.DiscordWebhookService
import snd.komf.notifications.discord.VelocityTemplateService

class ServerModule(
    private val appContext: AppContext,
    private val jobTracker: KomfJobTracker,
    private val jobsRepository: KomfJobsRepository,

    private val komgaMediaServerClient: StateFlow<MediaServerClient>,
    private val komgaMetadataServiceProvider: StateFlow<MetadataServiceProvider>,
    private val kavitaMetadataServiceProvider: StateFlow<MetadataServiceProvider>,
    private val kavitaMediaServerClient: StateFlow<MediaServerClient>,

    private val notificationService: StateFlow<DiscordWebhookService?>,
    private val velocityRenderer: StateFlow<VelocityTemplateService>,
) {
    private val configMapper = DeprecatedConfigUpdateMapper()
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val server = embeddedServer(CIO, port = appContext.appConfig.server.port) {
        install(ContentNegotiation) {
            json(json)
        }
        install(CORS) {
            anyMethod()
            allowHeaders { true }
            anyHost()
            allowNonSimpleContentTypes = true
        }
        install(SSE)


        routing {
            registerDeprecatedRoutes(this@embeddedServer)

            route("/api") {
                ConfigRoutes(appContext = appContext).registerRoutes(this)
                JobRoutes(
                    jobTracker = jobTracker,
                    jobsRepository = jobsRepository,
                    json = json
                ).registerRoutes(this)

                NotificationRoutes(
                    notificationService = notificationService,
                    templateRenderer = velocityRenderer
                ).registerRoutes(this)

                route("/komga") {
                    MetadataRoutes(
                        metadataServiceProvider = komgaMetadataServiceProvider,
                        mediaServerClient = komgaMediaServerClient,
                    ).registerRoutes(this)
                }

                route("/kavita") {
                    MetadataRoutes(
                        metadataServiceProvider = kavitaMetadataServiceProvider,
                        mediaServerClient = kavitaMediaServerClient,
                    ).registerRoutes(this)
                }

            }
        }
    }

    private fun registerDeprecatedRoutes(application: Application) {
        DeprecatedConfigRoutes(
            appContext = appContext,
            configMapper = configMapper
        ).registerRoutes(application)

        DeprecatedMetadataRoutes(
            metadataServiceProvider = komgaMetadataServiceProvider,
            mediaServerClient = komgaMediaServerClient,
            serverType = KOMGA
        ).registerRoutes(application)
        DeprecatedMetadataRoutes(
            metadataServiceProvider = kavitaMetadataServiceProvider,
            mediaServerClient = kavitaMediaServerClient,
            serverType = KAVITA
        ).registerRoutes(application)
    }

    fun startServer() {
        server.start(wait = true)
    }
}