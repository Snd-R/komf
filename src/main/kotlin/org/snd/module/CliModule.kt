package org.snd.module

import okhttp3.OkHttpClient
import org.snd.config.AppConfig

class CliModule(
    appConfig: AppConfig
) {
    private val okHttpClient = OkHttpClient.Builder().build()
    private val jsonModule = JsonModule()

    private val repositoryModule = RepositoryModule(appConfig.database)

    private val metadataModule = MetadataModule(
        config = appConfig.metadataProviders,
        okHttpClient = okHttpClient,
        jsonModule = jsonModule
    )

    val komgaModule = KomgaModule(
        config = appConfig.komga.copy(eventListener = appConfig.komga.eventListener.copy(enabled = false)),
        okHttpClient = okHttpClient,
        jsonModule = jsonModule,
        repositoryModule = repositoryModule,
        metadataModule = metadataModule,
        discordModule = null,
    )
}
