package org.snd

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.charleskorn.kaml.Yaml
import mu.KotlinLogging
import org.apache.commons.text.StringSubstitutor
import org.slf4j.LoggerFactory
import org.snd.config.AppConfig
import org.snd.module.AppModule
import java.lang.Runtime.getRuntime
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isReadable
import kotlin.system.exitProcess


private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    runCatching {
        val config = loadConfig(args.firstOrNull())
        val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.level = Level.valueOf(config.logLevel.uppercase())

        val appModule = AppModule(config)
        getRuntime().addShutdownHook(Thread { appModule.close() })
    }.getOrElse {
        logger.error(it) { "Failed to start the app" }
        exitProcess(1)
    }
}

fun loadConfig(configPath: String?): AppConfig {
    val config = loadFromEnv() ?: loadFromArgs(configPath) ?: loadDefault()
    val envResolved = StringSubstitutor.createInterpolator().replace(config)
    return Yaml.default.decodeFromString(AppConfig.serializer(), envResolved)
}

private fun loadFromEnv(): String? {
    val confEnv = System.getenv("KOMF_CONFIGDIR")
    return confEnv?.let {
        val path = Path.of(it)
        if (path.isReadable()) Files.readString(path)
        else null
    }
}

private fun loadFromArgs(configPath: String?): String? {
    return configPath?.let {
        Files.readString(Path.of(it).toRealPath())
    }
}

private fun loadDefault(): String {
    return AppConfig::class.java.getResource("/application.yml")!!.readText()
}
