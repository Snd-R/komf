package snd.komf.app

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.CompressedFileType
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import snd.komf.api.KomfErrorResponse
import snd.komf.app.api.ConfigRoutes
import snd.komf.app.api.JobRoutes
import snd.komf.app.api.MediaServerRoutes
import snd.komf.app.api.MetadataRoutes
import snd.komf.app.api.NotificationRoutes
import snd.komf.app.api.deprecated.DeprecatedConfigRoutes
import snd.komf.app.api.deprecated.DeprecatedConfigUpdateMapper
import snd.komf.app.api.deprecated.DeprecatedMetadataRoutes
import snd.komf.app.config.AppConfig
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.MetadataServiceProvider
import snd.komf.mediaserver.jobs.KomfJobTracker
import snd.komf.mediaserver.jobs.KomfJobsRepository
import snd.komf.mediaserver.model.MediaServer.KAVITA
import snd.komf.mediaserver.model.MediaServer.KOMGA
import snd.komf.notifications.apprise.AppriseCliService
import snd.komf.notifications.apprise.AppriseVelocityTemplates
import snd.komf.notifications.discord.DiscordVelocityTemplates
import snd.komf.notifications.discord.DiscordWebhookService
import snd.komf.providers.bookwalker.db.BookWalkerDbDownloader
import snd.komf.providers.mangabaka.db.MangaBakaDbDownloader
import snd.komf.providers.mangabaka.db.MangaBakaDbMetadata

class ServerModule(
    serverPort: Int,
    private val onConfigUpdate: suspend (AppConfig) -> Unit,
    private val dependencies: StateFlow<ApiRouteDependencies>,
) {

    private val configMapper = DeprecatedConfigUpdateMapper()
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val server = embeddedServer(CIO, port = serverPort) {
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
        install(DefaultHeaders) {
            header("Cross-Origin-Embedder-Policy", "require-corp")
            header("Cross-Origin-Opener-Policy", "same-origin")
        }
        install(StatusPages) {
            exception<IllegalStateException> { call, cause ->
                call.respond(
                    HttpStatusCode.InternalServerError,
                    KomfErrorResponse("${cause::class.simpleName} :${cause.message}")
                )
            }
            exception<IllegalArgumentException> { call, cause ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    KomfErrorResponse("${cause::class.simpleName} :${cause.message}")
                )
            }
        }

        routing {
            staticResources(remotePath = "/", basePackage = "komelia", index = "index.html") {
                default("index.html")
                preCompressed(CompressedFileType.GZIP)
            }

            registerDeprecatedRoutes(this@embeddedServer)

            route("/api") {
                ConfigRoutes(
                    config = dependencies.map { it.config },
                    onConfigUpdate = onConfigUpdate,
                    mangaBakaDownloader = dependencies.map { it.mangaBakaDownloader },
                    mangaBakaDbMetadata = dependencies.map { it.mangaBakaDbMetadata },
                    bookWalkerDbDownloader = dependencies.map { it.bookWalkerDbDownloader },
                    json = json,
                ).registerRoutes(this)
                JobRoutes(
                    jobTracker = dependencies.map { it.jobTracker },
                    jobsRepository = dependencies.map { it.jobsRepository },
                    json = json
                ).registerRoutes(this)

                NotificationRoutes(
                    discordService = dependencies.map { it.discordService },
                    discordRenderer = dependencies.map { it.discordRenderer },
                    appriseService = dependencies.map { it.appriseService },
                    appriseRenderer = dependencies.map { it.appriseRenderer }
                ).registerRoutes(this)

                route("/komga") {
                    MetadataRoutes(
                        metadataServiceProvider = dependencies.map { it.komgaMetadataServiceProvider },
                        mediaServerClient = dependencies.map { it.komgaMediaServerClient },
                    ).registerRoutes(this)

                    MediaServerRoutes(
                        mediaServerClient = dependencies.map { it.komgaMediaServerClient }
                    ).registerRoutes(this)
                }

                route("/kavita") {
                    MetadataRoutes(
                        metadataServiceProvider = dependencies.map { it.kavitaMetadataServiceProvider },
                        mediaServerClient = dependencies.map { it.kavitaMediaServerClient },
                    ).registerRoutes(this)

                    MediaServerRoutes(
                        mediaServerClient = dependencies.map { it.kavitaMediaServerClient }
                    ).registerRoutes(this)
                }
            }
        }
    }

    private fun registerDeprecatedRoutes(application: Application) {
        DeprecatedConfigRoutes(
            config = dependencies.map { it.config },
            onConfigUpdate = onConfigUpdate,
            configMapper = configMapper
        ).registerRoutes(application)

        DeprecatedMetadataRoutes(
            metadataServiceProvider = dependencies.map { it.komgaMetadataServiceProvider },
            mediaServerClient = dependencies.map { it.komgaMediaServerClient },
            jobTracker = dependencies.map { it.jobTracker },
            serverType = KOMGA
        ).registerRoutes(application)
        DeprecatedMetadataRoutes(
            metadataServiceProvider = dependencies.map { it.kavitaMetadataServiceProvider },
            mediaServerClient = dependencies.map { it.kavitaMediaServerClient },
            jobTracker = dependencies.map { it.jobTracker },
            serverType = KAVITA
        ).registerRoutes(application)
    }

    fun startServer() {
        server.start(wait = true)
    }
}

class ApiRouteDependencies(
    val config: AppConfig,
    val jobTracker: KomfJobTracker,
    val jobsRepository: KomfJobsRepository,
    val komgaMediaServerClient: MediaServerClient,
    val komgaMetadataServiceProvider: MetadataServiceProvider,
    val kavitaMediaServerClient: MediaServerClient,
    val kavitaMetadataServiceProvider: MetadataServiceProvider,
    val discordService: DiscordWebhookService,
    val discordRenderer: DiscordVelocityTemplates,
    val appriseService: AppriseCliService,
    val appriseRenderer: AppriseVelocityTemplates,
    val mangaBakaDownloader: MangaBakaDbDownloader,
    val mangaBakaDbMetadata: MangaBakaDbMetadata,
    val bookWalkerDbDownloader: BookWalkerDbDownloader,
)
