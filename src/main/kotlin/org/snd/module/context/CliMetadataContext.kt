package org.snd.module.context

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import org.snd.config.CalibreConfig
import org.snd.config.MetadataProvidersConfig
import org.snd.module.JsonModule
import org.snd.module.MetadataModule

class CliMetadataContext(
    config: MetadataProvidersConfig
) {
    private val okHttpClient = OkHttpClient.Builder().build()
    val jsonModule = JsonModule()
    val metadataModule: MetadataModule

    init {
        setLogLevel()
        metadataModule = MetadataModule(
            providersConfig = config,
            calibreConfig = CalibreConfig(),
            okHttpClient = okHttpClient,
            jsonModule = jsonModule
        )
    }

    private fun setLogLevel() {
        val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.level = Level.ERROR
    }

}