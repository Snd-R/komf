package snd.komf.app.api.deprecated

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import snd.komf.app.api.deprecated.dto.AppConfigUpdateDto
import snd.komf.app.config.AppConfig

class DeprecatedConfigRoutes(
    private val config: Flow<AppConfig>,
    private val onConfigUpdate: suspend (AppConfig) -> Unit,
    private val configMapper: DeprecatedConfigUpdateMapper,
) {

    fun registerRoutes(application: Application) {
        application.routing {
            getConfigRoute()
            updateConfigRoute()
        }
    }

    private fun Routing.getConfigRoute() {
        get("/config") {
            val config = configMapper.toDto(config.first())
            call.respond(config)
        }
    }

    private fun Routing.updateConfigRoute() {
        patch("/config") {
            val request = call.receive<AppConfigUpdateDto>()
            val config = configMapper.patch(config.first(), request)

            try {
                onConfigUpdate(config)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, "${e::class.simpleName}: ${e.message}")
                return@patch
            }

            call.response.status(HttpStatusCode.NoContent)
        }
    }
}