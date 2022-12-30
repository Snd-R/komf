package org.snd.module

import ch.qos.logback.classic.Level.DEBUG
import ch.qos.logback.classic.Level.ERROR
import ch.qos.logback.classic.Level.INFO
import ch.qos.logback.classic.Logger
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory.getLogger
import org.snd.config.AppConfig
import org.snd.config.ConfigLoader
import org.snd.mediaserver.model.MediaServer
import java.nio.file.Path
import kotlin.io.path.isDirectory

class CliContext(
    configPath: Path? = null,
    mediaServer: MediaServer,
    verbose: Boolean = false
) {
    val cliModule: CliModule
    private val configLoader = ConfigLoader()

    init {
        setLogLevel(verbose)
        val config = loadConfig(configPath)
        cliModule = CliModule(config, mediaServer)
    }

    private fun loadConfig(path: Path?): AppConfig {
        return when {
            path == null -> configLoader.default()
            path.isDirectory() -> configLoader.loadDirectory(path)
            else -> configLoader.loadFile(path)
        }
    }

    private fun setLogLevel(verbose: Boolean) {
        val rootLogger = getLogger(ROOT_LOGGER_NAME) as Logger
        if (verbose) {
            rootLogger.level = DEBUG
        } else {
            rootLogger.level = ERROR
            (getLogger("org.snd") as Logger).level = INFO
        }
    }
}