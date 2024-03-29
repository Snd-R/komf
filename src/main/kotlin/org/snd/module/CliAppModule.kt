package org.snd.module

import okhttp3.OkHttpClient
import org.snd.config.AppConfig
import org.snd.mediaserver.model.mediaserver.MediaServer
import org.snd.mediaserver.model.mediaserver.MediaServer.KAVITA
import org.snd.mediaserver.model.mediaserver.MediaServer.KOMGA
import java.time.Clock

class CliAppModule(
    appConfig: AppConfig,
    serverType: MediaServer = KOMGA,
) {
    private val okHttpClient = OkHttpClient.Builder().build()
    private val jsonModule = JsonModule()

    private val repositoryModule = RepositoryModule(appConfig.database)

    private val metadataModule = MetadataModule(
        providersConfig = appConfig.metadataProviders,
        calibreConfig = appConfig.calibre,
        okHttpClient = okHttpClient,
        jsonModule = jsonModule
    )

    private val mediaServerModule = MediaServerModule(
        komgaConfig = appConfig.komga.copy(eventListener = appConfig.komga.eventListener.copy(enabled = false)),
        kavitaConfig = appConfig.kavita.copy(eventListener = appConfig.kavita.eventListener.copy(enabled = false)),
        okHttpClient = okHttpClient,
        jsonModule = jsonModule,
        repositoryModule = repositoryModule,
        metadataModule = metadataModule,
        notificationsModule = null,
        clock = Clock.systemUTC(),
        systemDefaultClock = Clock.systemDefaultZone()
    )
    val mediaServerClient = when (serverType) {
        KOMGA -> mediaServerModule.komgaMediaServerClient
        KAVITA -> mediaServerModule.kavitaMediaServerClient
    }

    val metadataServiceProvider = when (serverType) {
        KOMGA -> mediaServerModule.komgaMetadataServiceProvider
        KAVITA -> mediaServerModule.kavitaMetadataServiceProvider
    }
    val metadataUpdateServiceProvider = when (serverType) {
        KOMGA -> mediaServerModule.komgaMetadataUpdateServiceProvider
        KAVITA -> mediaServerModule.kavitaMetadataUpdateServiceProvider
    }
}
