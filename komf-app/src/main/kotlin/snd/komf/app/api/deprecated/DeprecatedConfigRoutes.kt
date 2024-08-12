package snd.komf.app.api.deprecated

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import snd.komf.app.AppContext
import snd.komf.app.api.deprecated.dto.AppConfigUpdateDto

class DeprecatedConfigRoutes(
    private val appContext: AppContext,
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
            val config = configMapper.toDto(appContext.appConfig)
            call.respond(config)
        }
    }

    private fun Routing.updateConfigRoute() {
        patch("/config") {
            val request = call.receive<AppConfigUpdateDto>()
            val config = configMapper.patch(appContext.appConfig, request)

            try {
                appContext.updateConfig(config)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.UnprocessableEntity, "${e::class.simpleName}: ${e.message}")
                return@patch
            }

            call.response.status(HttpStatusCode.NoContent)
        }
    }
}