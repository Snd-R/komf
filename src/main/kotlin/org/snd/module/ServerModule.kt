package org.snd.module

import com.charleskorn.kaml.Yaml
import io.javalin.Javalin
import io.javalin.util.ConcurrencyUtil
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.eclipse.jetty.server.LowResourceMonitor
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.snd.api.ConfigController
import org.snd.api.ConfigUpdateMapper
import org.snd.api.MetadataController
import org.snd.config.ConfigWriter
import org.snd.config.ServerConfig
import org.snd.mediaserver.model.MediaServer.KAVITA
import org.snd.mediaserver.model.MediaServer.KOMGA
import org.snd.module.context.AppContext
import java.util.concurrent.Executors

class ServerModule(
    config: ServerConfig,
    appContext: AppContext,
    mediaServerModule: MediaServerModule,
    jsonModule: JsonModule,
) : AutoCloseable {
    private val executor = Executors.newSingleThreadExecutor(
        BasicThreadFactory.Builder()
            .namingPattern("komf-api-task-handler-%d").build()
    )

    private val configWriter = ConfigWriter(Yaml.default)
    private val configMapper = ConfigUpdateMapper()

    private val jetty = Server(ConcurrencyUtil.jettyThreadPool("JettyServerThreadPool", 1, 20))
        .apply {
            addBean(LowResourceMonitor(this))
            insertHandler(StatisticsHandler())
            stopTimeout = 30000L
        }
    private val server = Javalin.create { config ->
        config.plugins.enableCors { cors -> cors.add { it.anyHost() } }
        config.showJavalinBanner = false
        config.jetty.server { jetty }
    }.routes {
        MetadataController(
            metadataServiceProvider = mediaServerModule.komgaMetadataServiceProvider,
            metadataUpdateServiceProvider = mediaServerModule.komgaMetadataUpdateServiceProvider,
            mediaServerClient = mediaServerModule.komgaMediaServerClient,
            taskHandler = executor,
            moshi = jsonModule.moshi,
            serverType = KOMGA
        ).register()
        MetadataController(
            metadataServiceProvider = mediaServerModule.kavitaMetadataServiceProvider,
            metadataUpdateServiceProvider = mediaServerModule.kavitaMetadataUpdateServiceProvider,
            mediaServerClient = mediaServerModule.komgaMediaServerClient,
            taskHandler = executor,
            moshi = jsonModule.moshi,
            serverType = KAVITA
        ).register()

        ConfigController(
            appContext = appContext,
            configWriter = configWriter,
            moshi = jsonModule.moshi,
            configMapper = configMapper
        ).register()
    }

    init {
        try {
            server.start(config.port)
        } catch (e: Exception) {
            if (jetty.isStarted || jetty.isStarting)
                jetty.stop()
            throw e
        }
    }

    override fun close() {
        executor.shutdown()

        while (true) {
            try {
                if (!jetty.isStopped || !jetty.isFailed)
                    server.close()
                return
            } catch (e: Exception) {
                //ignore
            }
        }
    }
}
