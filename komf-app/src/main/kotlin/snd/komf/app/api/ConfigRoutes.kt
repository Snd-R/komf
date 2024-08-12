package snd.komf.app.api

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import snd.komf.api.config.KomfConfigUpdateRequest
import snd.komf.app.AppContext
import snd.komf.app.api.mappers.AppConfigMapper
import snd.komf.app.api.mappers.AppConfigUpdateMapper


class ConfigRoutes(
    private val appContext: AppContext,
) {
    private val configMapper = AppConfigMapper()
    private val updateConfigMapper = AppConfigUpdateMapper()
    private val mutex = Mutex()

    fun registerRoutes(routing: Route) {
        with(routing) {
            getConfigRoute()
            updateConfigRoute()
        }
    }

    private fun Route.getConfigRoute() {
        get("/config") {
            call.respond(configMapper.toDto(appContext.appConfig))
        }
    }

    private fun Route.updateConfigRoute() {
        patch("/config") {
            val request = call.receive<KomfConfigUpdateRequest>()
            mutex.withLock {
                val config = updateConfigMapper.patch(appContext.appConfig, request)
                try {
                    appContext.updateConfig(config)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.UnprocessableEntity, "${e::class.simpleName}: ${e.message}")
                    return@patch
                }

            }
            call.response.status(HttpStatusCode.NoContent)
        }
    }
}
