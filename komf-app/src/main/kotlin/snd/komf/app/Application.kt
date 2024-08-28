package snd.komf.app

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    runCatching {
        val configFile = args.firstOrNull()?.let { Path.of(it) }
        val configDir = System.getenv("KOMF_CONFIG_DIR")?.let { Path.of(it) }
        AppContext(configDir?: configFile)
    }.getOrElse {
        logger.error(it) { "Failed to start the app" }
        exitProcess(1)
    }
}
