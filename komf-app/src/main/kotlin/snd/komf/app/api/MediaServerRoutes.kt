package snd.komf.app.api

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import snd.komf.api.mediaserver.KomfMediaServerConnectionResponse
import snd.komf.api.mediaserver.KomfMediaServerLibrary
import snd.komf.api.mediaserver.KomfMediaServerLibraryId
import snd.komf.mediaserver.MediaServerClient

class MediaServerRoutes(
    private val mediaServerClient: Flow<MediaServerClient>,
) {

    fun registerRoutes(routing: Route) {
        routing.route("/media-server") {
            checkConnectionRoute()
            getLibrariesRoute()
        }
    }

    private fun Route.checkConnectionRoute() {
        get("/connected") {
            try {
                mediaServerClient.first().getLibraries()
                call.respond(
                    HttpStatusCode.OK,
                    KomfMediaServerConnectionResponse(
                        success = true,
                        httpStatusCode = HttpStatusCode.OK.value,
                        errorMessage = null
                    )
                )
            } catch (exception: ResponseException) {
                call.respond(
                    HttpStatusCode.OK,
                    KomfMediaServerConnectionResponse(
                        success = false,
                        httpStatusCode = exception.response.status.value,
                        errorMessage = HttpStatusCode.fromValue(exception.response.status.value).description
                    )
                )
            } catch (exception: Exception) {
                call.respond(
                    HttpStatusCode.OK,
                    KomfMediaServerConnectionResponse(
                        success = false,
                        httpStatusCode = null,
                        errorMessage =
                            buildString {
                                exception.message?.let { append(it) }
                                exception.cause?.message?.let { append("; $it") }
                            }
                    )
                )

            }
        }
    }

    private fun Route.getLibrariesRoute() {
        get("/libraries") {
            val libraries = mediaServerClient.first().getLibraries().map {
                KomfMediaServerLibrary(
                    id = KomfMediaServerLibraryId(it.id.value),
                    name = it.name,
                    roots = it.roots
                )
            }
            call.respond(HttpStatusCode.OK, libraries)
        }
    }
}