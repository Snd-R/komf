package org.snd.module

import io.javalin.Javalin
import org.snd.api.KomfController
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
        KomfController(
            komgaService = komgaModule.komgaService,
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
