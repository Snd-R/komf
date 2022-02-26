package org.snd.module

import org.snd.config.AppConfig

class AppModule(
    appConfig: AppConfig
) : AutoCloseable {
    private val jsonModule = JsonModule()

     val metadataModule = MetadataModule(
        config = appConfig.metadataProviders,
        jsonModule = jsonModule
    )

    private val komgaModule = KomgaModule(
        config = appConfig.komga,
        jsonModule = jsonModule,
        metadataModule = metadataModule
    )

    private val serverModule = ServerModule(
        config = appConfig.server,
        komgaModule = komgaModule,
        jsonModule = jsonModule
    )

    override fun close() {
        serverModule.close()
        komgaModule.close()
    }
}
