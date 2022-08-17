package org.snd.module

import io.javalin.Javalin
import org.snd.api.KomgaMetadataController
import org.snd.config.ServerConfig
import java.util.concurrent.Executors

class ServerModule(
    config: ServerConfig,
    komgaModule: KomgaModule,
    jsonModule: JsonModule
) : AutoCloseable {
    private val server = Javalin.create { config ->
        config.enableCorsForAllOrigins()
        config.showJavalinBanner = false
    }.routes {
        KomgaMetadataController(
            komgaMetadataService = komgaModule.komgaMetadataService,
            taskHandler = Executors.newSingleThreadExecutor(),
            moshi = jsonModule.moshi
        ).register()
    }

    init {
        server.start(config.port)
    }

    override fun close() {
        server.stop()
    }
}
