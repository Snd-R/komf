package snd.komf.app.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import snd.komf.api.KomfErrorResponse
import snd.komf.api.config.KomfConfigUpdateRequest
import snd.komf.app.api.mappers.AppConfigMapper
import snd.komf.app.api.mappers.AppConfigUpdateMapper
import snd.komf.app.config.AppConfig
import snd.komf.providers.mangabaka.db.MangaBakaDbDownloader
import snd.komf.providers.mangabaka.db.MangaBakaDbMetadata
import snd.komf.providers.mangabaka.db.MangaBakaDownloadProgress.ErrorEvent
import snd.komf.providers.mangabaka.db.MangaBakaDownloadProgress.FinishedEvent
import snd.komf.providers.mangabaka.db.MangaBakaDownloadProgress.ProgressEvent

private val logger = KotlinLogging.logger {}

class ConfigRoutes(
    private val config: Flow<AppConfig>,
    private val onConfigUpdate: suspend (AppConfig) -> Unit,
    private val mangaBakaDownloader: Flow<MangaBakaDbDownloader>,
    private val mangaBakaDbMetadata: Flow<MangaBakaDbMetadata>,
    private val json: Json,
) {
    private val configMapper = AppConfigMapper()
    private val updateConfigMapper = AppConfigUpdateMapper()
    private val mutex = Mutex()

    fun registerRoutes(routing: Route) {
        with(routing) {
            getConfigRoute()
            updateConfigRoute()
            updateMangaBakaDB()
        }
    }

    private fun Route.getConfigRoute() {
        get("/config") {
            call.respond(
                configMapper.toDto(
                    config = config.first(),
                    mangaBakaDbMetadata = mangaBakaDbMetadata.first()
                )
            )
        }
    }

    private fun Route.updateConfigRoute() {
        patch("/config") {
            mutex.withLock {
                val request = call.receive<KomfConfigUpdateRequest>()
                val updatedConfig = updateConfigMapper.patch(config.first(), request)
                try {
                    onConfigUpdate(updatedConfig)
                } catch (e: Exception) {
                    logger.catching(e)
                    call.respond(
                        HttpStatusCode.UnprocessableEntity,
                        KomfErrorResponse("${e::class.simpleName}: ${e.message}")
                    )
                    return@patch
                }

            }
            call.response.status(HttpStatusCode.NoContent)
        }
    }

    private fun Route.updateMangaBakaDB() {
        post("/update-manga-baka-db") {
            val downloader = mangaBakaDownloader.first()

            call.respondBytesWriter(contentType = ContentType("application", "jsonl")) {
                downloader.launchDownload().collect { event ->
                    val mappedEvent = when (event) {
                        is ProgressEvent -> snd.komf.api.config.MangaBakaDownloadProgress.ProgressEvent(
                            event.total,
                            event.completed,
                            event.info
                        )

                        is ErrorEvent -> snd.komf.api.config.MangaBakaDownloadProgress.ErrorEvent(event.message)
                        FinishedEvent -> snd.komf.api.config.MangaBakaDownloadProgress.FinishedEvent
                    }
                    writeStringUtf8(json.encodeToString(mappedEvent) + "\n")
                    flush()
                }
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}
