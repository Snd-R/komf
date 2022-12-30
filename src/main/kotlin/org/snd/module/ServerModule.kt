package org.snd.module

import io.javalin.Javalin
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.snd.api.MetadataController
import org.snd.config.ServerConfig
import org.snd.mediaserver.model.MediaServer.KAVITA
import org.snd.mediaserver.model.MediaServer.KOMGA
import java.util.concurrent.Executors

class ServerModule(
    config: ServerConfig,
    mediaServerModule: MediaServerModule,
    jsonModule: JsonModule,
    appContext: AppContext
) : AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor(
        BasicThreadFactory.Builder()
            .namingPattern("komf-api-task-handler-%d").build()
    )

    private val server = Javalin.create { config ->
        config.plugins.enableCors { cors -> cors.add { it.anyHost() } }
        config.showJavalinBanner = false
    }.routes {
        MetadataController(
            metadataService = mediaServerModule.komgaMetadataService,
            metadataUpdateService = mediaServerModule.komgaMetadataUpdateService,
            taskHandler = executor,
            moshi = jsonModule.moshi,
            serverType = KOMGA
        ).register()
        MetadataController(
            metadataService = mediaServerModule.kavitaMetadataService,
            metadataUpdateService = mediaServerModule.kavitaMetadataUpdateService,
            taskHandler = executor,
            moshi = jsonModule.moshi,
            serverType = KAVITA
        ).register()
    }

    init {
        server.start(config.port)
    }

    override fun close() {
        server.stop()
    }
}
