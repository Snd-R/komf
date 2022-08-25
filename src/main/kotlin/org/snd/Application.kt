package org.snd

import com.github.ajalt.clikt.core.subcommands
import mu.KotlinLogging
import org.snd.cli.Komf
import org.snd.cli.Library
import org.snd.cli.Series
import kotlin.system.exitProcess


private val logger = KotlinLogging.logger {}

fun main(vararg args: String) {
    runCatching {
        Komf().subcommands(
            Series().subcommands(Series.Search(), Series.Update(), Series.Identify(), Series.Reset()),
            Library().subcommands(Library.Update(), Library.Reset())
        ).main(args)
    }.getOrElse {
        logger.error(it) { "Failed to start the app" }
        exitProcess(1)
    }
}
