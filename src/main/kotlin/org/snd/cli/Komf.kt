package org.snd.cli

import ch.qos.logback.classic.Level.*
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.slf4j.LoggerFactory.getLogger
import org.snd.config.AppConfig
import org.snd.infra.ConfigLoader
import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServer.KOMGA
import org.snd.module.AppModule
import org.snd.module.CliModule
import java.nio.file.Path


class Komf : CliktCommand(invokeWithoutSubcommand = true) {
    private val configDir by option(envvar = "KOMF_CONFIG_DIR").convert { Path.of(it) }
    private val configFile by option().convert { Path.of(it) }
    private val configFileArgument by argument().convert { Path.of(it) }.optional()
    private val verbose by option().flag()
    private val mediaServer by option().convert { MediaServer.valueOf(it.uppercase()) }.default(KOMGA)

    override fun run() {
        val config = ConfigLoader().loadConfig(configFile ?: configFileArgument, configDir)

        if (currentContext.invokedSubcommand == null) {
            setAppLogLevel(verbose, config)
            val appModule = AppModule(config)
            Runtime.getRuntime().addShutdownHook(Thread { appModule.close() })
        } else {
            setCliLogLevel(verbose)
            currentContext.findOrSetObject { CliModule(config, mediaServer) }
        }
    }

    private fun setAppLogLevel(verbose: Boolean, config: AppConfig) {
        val rootLogger = getLogger(ROOT_LOGGER_NAME) as Logger
        if (verbose) rootLogger.level = DEBUG
        else {
            val logLevel = valueOf(config.logLevel.uppercase())
            if (logLevel == INFO) {
                (getLogger("org.flywaydb") as Logger).level = WARN
                (getLogger("com.zaxxer.hikari") as Logger).level = WARN
            }

            rootLogger.level = valueOf(config.logLevel.uppercase())
        }
        (getLogger("org.eclipse.jetty") as Logger).level = WARN
    }

    private fun setCliLogLevel(verbose: Boolean) {
        val rootLogger = getLogger(ROOT_LOGGER_NAME) as Logger
        if (verbose) {
            rootLogger.level = DEBUG
        } else {
            rootLogger.level = ERROR
            (getLogger("org.snd") as Logger).level = INFO
        }
    }
}
