package org.snd

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.Flyway.configure
import org.slf4j.LoggerFactory
import org.snd.config.AppConfig
import org.snd.infra.ConfigLoader
import org.snd.module.AppModule
import java.lang.Runtime.getRuntime
import java.nio.file.Path
import kotlin.system.exitProcess


private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    runCatching {
        val config = ConfigLoader()
            .loadConfig(args.firstOrNull()?.let { Path.of(it) })
        val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.level = Level.valueOf(config.logLevel.uppercase())

        applyMigrations(config)
        val appModule = AppModule(config)
        getRuntime().addShutdownHook(Thread { appModule.close() })
    }.getOrElse {
        logger.error(it) { "Failed to start the app" }
        exitProcess(1)
    }
}

private fun applyMigrations(appConfig: AppConfig) {
    Flyway(
        configure()
            .dataSource("jdbc:sqlite:${appConfig.database.file}", null, null)
            .locations("classpath:db/migration/sqlite")
    ).migrate()
}
