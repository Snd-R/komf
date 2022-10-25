package org.snd.module

import io.javalin.Javalin
import org.snd.api.KomgaMetadataController
import org.snd.config.ServerConfig
import java.util.concurrent.Executors

class ServerModule(
    config: ServerConfig,
    mediaServerModule: MediaServerModule,
    jsonModule: JsonModule
) : AutoCloseable {
    private val server = Javalin.create { config ->
        config.plugins.enableCors { cors -> cors.add { it.anyHost() } }
        config.showJavalinBanner = false
    }.routes {
        mediaServerModule.komgaMetadataService?.let {
            KomgaMetadataController(
                komgaMetadataService = mediaServerModule.komgaMetadataService,
                taskHandler = Executors.newSingleThreadExecutor(),
                moshi = jsonModule.moshi
            ).register()
        }
    }

    init {
        server.start(config.port)
    }

    override fun close() {
        server.stop()
    }
}
