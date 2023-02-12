package org.snd.module

import okhttp3.OkHttpClient
import org.snd.config.AppConfig
import org.snd.module.context.AppContext
import java.time.Clock

class AppModule(
    appConfig: AppConfig,
    context: AppContext
) : AutoCloseable {
    private val okHttpClient = OkHttpClient.Builder().build()
    private val jsonModule = JsonModule()

    private val repositoryModule = RepositoryModule(appConfig.database)

    private val notificationsModule = NotificationsModule(
        config = appConfig.discord,
        okHttpClient = okHttpClient,
        jsonModule = jsonModule,
    )

    private val metadataModule = MetadataModule(
        config = appConfig.metadataProviders,
        okHttpClient = okHttpClient,
        jsonModule = jsonModule
    )

    private val mediaServerModule = MediaServerModule(
        komgaConfig = appConfig.komga,
        kavitaConfig = appConfig.kavita,
        okHttpClient = okHttpClient,
        jsonModule = jsonModule,
        repositoryModule = repositoryModule,
        metadataModule = metadataModule,
        notificationsModule = notificationsModule,
        clock = Clock.systemUTC(),
        systemDefaultClock = Clock.systemDefaultZone()
    )

    private val serverModule = ServerModule(
        config = appConfig.server,
        appContext = context,
        mediaServerModule = mediaServerModule,
        jsonModule = jsonModule,
    )

    override fun close() {

        serverModule.close()
        metadataModule.close()
        mediaServerModule.close()
        repositoryModule.close()

        okHttpClient.dispatcher.executorService.shutdown()
        okHttpClient.connectionPool.evictAll()
        okHttpClient.cache?.close()
    }
}
