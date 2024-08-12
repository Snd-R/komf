package snd.komf.app.api.deprecated

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.flow.StateFlow
import snd.komf.app.api.deprecated.dto.IdentifySeriesRequest
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.MetadataServiceProvider
import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.model.ProviderSeriesId
import snd.komf.providers.CoreProviders

class DeprecatedMetadataRoutes(
    private val metadataServiceProvider: StateFlow<MetadataServiceProvider>,
    private val mediaServerClient: StateFlow<MediaServerClient>,
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
                        ?.let { metadataServiceProvider.value.fetcherServiceFor(it.value).availableProviders(it) }
                        ?: metadataServiceProvider.value.defaultFetcherService().availableProviders()
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
                ?: seriesId?.let { mediaServerClient.value.getSeries(it).libraryId }

            val searchResults = libraryId
                ?.let { metadataServiceProvider.value.fetcherServiceFor(it.value).searchSeriesMetadata(seriesName, it) }
                ?: metadataServiceProvider.value.defaultFetcherService().searchSeriesMetadata(seriesName)

            call.respond(HttpStatusCode.OK, searchResults)
        }
    }

    private fun Route.identifySeriesRoute() {
        post("/identify") {
            val request = call.receive<IdentifySeriesRequest>()

            val libraryId = request.libraryId
                ?: mediaServerClient.value.getSeries(MediaServerSeriesId(request.seriesId)).libraryId.value

            metadataServiceProvider.value.fetcherServiceFor(libraryId).setSeriesMetadata(
                MediaServerSeriesId(request.seriesId),
                CoreProviders.valueOf(request.provider.uppercase()),
                ProviderSeriesId(request.providerSeriesId),
                request.edition
            )

            call.response.status(HttpStatusCode.NoContent)
        }
    }

    private fun Route.matchSeriesRoute() {
        post("/match/library/{libraryId}/series/{seriesId}") {

            val libraryId = call.parameters.getOrFail("libraryId")
            val seriesId = MediaServerSeriesId(call.parameters.getOrFail("seriesId"))
            metadataServiceProvider.value.fetcherServiceFor(libraryId).matchSeriesMetadata(seriesId)
            call.response.status(HttpStatusCode.NoContent)
        }
    }

    private fun Route.matchLibraryRoute() {
        post("/match/library/{libraryId}") {
            val libraryId = MediaServerLibraryId(call.parameters.getOrFail("libraryId"))
            metadataServiceProvider.value.fetcherServiceFor(libraryId.value).matchLibraryMetadata(libraryId)
            call.response.status(HttpStatusCode.Accepted)
        }
    }

    private fun Route.resetSeriesRoute() {
        post("/reset/library/{libraryId}/series/{seriesId}") {
            val libraryId = call.parameters.getOrFail("libraryId")
            val seriesId = MediaServerSeriesId(call.parameters.getOrFail("seriesId"))
            val removeComicInfo = call.queryParameters["removeComicInfo"].toBoolean()
            metadataServiceProvider.value.updateServiceFor(libraryId).resetSeriesMetadata(seriesId, removeComicInfo)
            call.response.status(HttpStatusCode.NoContent)
        }
    }

    private fun Route.resetLibraryRoute() {
        post("/reset/library") {
            val libraryId = MediaServerLibraryId(call.parameters.getOrFail("libraryId"))
            val removeComicInfo = call.queryParameters["removeComicInfo"].toBoolean()
            metadataServiceProvider.value.updateServiceFor(libraryId.value)
                .resetLibraryMetadata(libraryId, removeComicInfo)
            call.response.status(HttpStatusCode.NoContent)
        }
    }

}