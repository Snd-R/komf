package org.snd.module.context

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Level.DEBUG
import ch.qos.logback.classic.Level.INFO
import ch.qos.logback.classic.Level.WARN
import ch.qos.logback.classic.Logger
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory.getLogger
import org.snd.config.AppConfig
import org.snd.config.ConfigLoader
import org.snd.module.AppModule
import java.nio.file.Path
import kotlin.io.path.isDirectory

class AppContext(val configPath: Path? = null) {
    var verbose: Boolean = false

    @Volatile
    lateinit var appConfig: AppConfig
        private set

    private val configLoader = ConfigLoader()

    @Volatile
    private lateinit var appModule: AppModule

    fun init() {
        refresh()
        Runtime.getRuntime().addShutdownHook(Thread { close() })
    }

    @Synchronized
    fun refresh() {
        close()
        val config = loadConfig()
        setLogLevel(config)
        appConfig = config
        appModule = AppModule(config, this)
    }

    @Synchronized
    private fun close() {
        if (::appModule.isInitialized) {
            appModule.close()
        }
    }

    private fun loadConfig(): AppConfig {
        return when {
            configPath == null -> configLoader.default()
            configPath.isDirectory() -> configLoader.loadDirectory(configPath)
            else -> configLoader.loadFile(configPath)
        }
    }

    private fun setLogLevel(config: AppConfig) {
        val rootLogger = getLogger(ROOT_LOGGER_NAME) as Logger
        if (verbose) rootLogger.level = DEBUG
        else {
            val logLevel = Level.valueOf(config.logLevel.uppercase())
            if (logLevel == INFO) {
                (getLogger("org.flywaydb") as Logger).level = WARN
                (getLogger("com.zaxxer.hikari") as Logger).level = WARN
            }

            rootLogger.level = Level.valueOf(config.logLevel.uppercase())
        }
        (getLogger("org.eclipse.jetty") as Logger).level = WARN
    }
}