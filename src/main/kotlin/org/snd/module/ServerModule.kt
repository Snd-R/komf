package org.snd.module

import io.javalin.Javalin
import org.snd.api.MetadataController
import org.snd.config.ServerConfig
import org.snd.mediaserver.model.MediaServer.KAVITA
import org.snd.mediaserver.model.MediaServer.KOMGA
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
        MetadataController(
            metadataService = mediaServerModule.komgaMetadataService,
            taskHandler = Executors.newSingleThreadExecutor(),
            moshi = jsonModule.moshi,
            KOMGA
        ).register()
        MetadataController(
            metadataService = mediaServerModule.kavitaMetadataService,
            taskHandler = Executors.newSingleThreadExecutor(),
            moshi = jsonModule.moshi,
            KAVITA
        ).register()
    }

    init {
        server.start(config.port)
    }

    override fun close() {
        server.stop()
    }
}
