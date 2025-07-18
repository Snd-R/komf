package snd.komf.app.api.deprecated

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import snd.komf.app.api.deprecated.dto.IdentifySeriesRequest
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.MetadataServiceProvider
import snd.komf.mediaserver.jobs.KomfJobTracker
import snd.komf.mediaserver.jobs.MetadataJobEvent.CompletionEvent
import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.model.ProviderSeriesId
import snd.komf.providers.CoreProviders

class DeprecatedMetadataRoutes(
    private val metadataServiceProvider: Flow<MetadataServiceProvider>,
    private val mediaServerClient: Flow<MediaServerClient>,
    private val jobTracker: Flow<KomfJobTracker>,
    private val serverType: MediaServer,
) {

    fun registerRoutes(application: Application) {
        application.routing {
            route("/${serverType.name.lowercase()}") {
                getProvidersRoute()
                searchSeriesRoute()
                identifySeriesRoute()
                matchSeriesRoute()
                matchLibraryRoute()
                resetSeriesRoute()
                resetLibraryRoute()
            }
        }
    }

    private fun Route.getProvidersRoute() {
        get("/providers") {
            val libraryId = call.request.queryParameters["libraryId"]?.let { MediaServerLibraryId(it) }

            val providers = (
                    libraryId
                        ?.let { metadataServiceProvider.first().metadataServiceFor(it.value).availableProviders(it) }
                        ?: metadataServiceProvider.first().defaultMetadataService().availableProviders()
                    )
                .map { it.providerName().name }

            call.respond(providers)
        }
    }

    private fun Route.searchSeriesRoute() {
        get("/search") {
            val seriesName = call.request.queryParameters["name"]
                ?: return@get call.response.status(HttpStatusCode.BadRequest)

            val seriesId = call.request.queryParameters["seriesId"]?.let { MediaServerSeriesId(it) }
            val libraryId = call.request.queryParameters["libraryId"]
                ?.let { MediaServerLibraryId(it) }
                ?: seriesId?.let { mediaServerClient.first().getSeries(it).libraryId }

            val searchResults = libraryId
                ?.let {
                    metadataServiceProvider.first().metadataServiceFor(it.value).searchSeriesMetadata(seriesName, it)
                }
                ?: metadataServiceProvider.first().defaultMetadataService().searchSeriesMetadata(seriesName)

            call.respond(HttpStatusCode.OK, searchResults)
        }
    }

    private fun Route.identifySeriesRoute() {
        post("/identify") {
            val request = call.receive<IdentifySeriesRequest>()

            val libraryId = request.libraryId
                ?: mediaServerClient.first().getSeries(MediaServerSeriesId(request.seriesId)).libraryId.value

            val jobId = metadataServiceProvider.first().metadataServiceFor(libraryId).setSeriesMetadata(
                MediaServerSeriesId(request.seriesId),
                CoreProviders.valueOf(request.provider.uppercase()),
                ProviderSeriesId(request.providerSeriesId),
                request.edition
            )
            jobTracker.first().getMetadataJobEvents(jobId)
                ?.takeWhile { it != CompletionEvent }
                ?.collect {}

            call.response.status(HttpStatusCode.NoContent)
        }
    }

    private fun Route.matchSeriesRoute() {
        post("/match/library/{libraryId}/series/{seriesId}") {

            val libraryId = call.parameters.getOrFail("libraryId")
            val seriesId = MediaServerSeriesId(call.parameters.getOrFail("seriesId"))
            val jobId = metadataServiceProvider.first().metadataServiceFor(libraryId).matchSeriesMetadata(seriesId)
            jobTracker.first().getMetadataJobEvents(jobId)
                ?.takeWhile { it != CompletionEvent }
                ?.collect {}

            call.response.status(HttpStatusCode.NoContent)
        }
    }

    private fun Route.matchLibraryRoute() {
        post("/match/library/{libraryId}") {
            val libraryId = MediaServerLibraryId(call.parameters.getOrFail("libraryId"))
            metadataServiceProvider.first().metadataServiceFor(libraryId.value).matchLibraryMetadata(libraryId)
            call.response.status(HttpStatusCode.Accepted)
        }
    }

    private fun Route.resetSeriesRoute() {
        post("/reset/library/{libraryId}/series/{seriesId}") {
            val libraryId = call.parameters.getOrFail("libraryId")
            val seriesId = MediaServerSeriesId(call.parameters.getOrFail("seriesId"))
            val removeComicInfo = call.queryParameters["removeComicInfo"].toBoolean()
            metadataServiceProvider.first().updateServiceFor(libraryId).resetSeriesMetadata(seriesId, removeComicInfo)
            call.response.status(HttpStatusCode.NoContent)
        }
    }

    private fun Route.resetLibraryRoute() {
        post("/reset/library/{libraryId}") {
            val libraryId = MediaServerLibraryId(call.parameters.getOrFail("libraryId"))
            val removeComicInfo = call.queryParameters["removeComicInfo"].toBoolean()
            metadataServiceProvider.first().updateServiceFor(libraryId.value)
                .resetLibraryMetadata(libraryId, removeComicInfo)
            call.response.status(HttpStatusCode.NoContent)
        }
    }

}