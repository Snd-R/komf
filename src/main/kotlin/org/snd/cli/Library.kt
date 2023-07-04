package org.snd.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import org.snd.mediaserver.model.mediaserver.MediaServerLibraryId
import org.snd.module.context.CliAppContext
import kotlin.system.exitProcess

class Library : CliktCommand() {
    override fun run() {}

    class Update : CliktCommand() {
        private val context by requireObject<CliAppContext>()
        private val id by argument()
        override fun run() {
            context.cliAppModule.metadataServiceProvider.serviceFor(id)
                .matchLibraryMetadata(MediaServerLibraryId(id))
            exitProcess(0)
        }
    }

    class Reset : CliktCommand() {
        private val context by requireObject<CliAppContext>()
        private val removeComicInfo by option().convert { it.toBoolean() }.default(false)
        private val id by argument()
        override fun run() {
            context.cliAppModule.metadataUpdateServiceProvider.serviceFor(id)
                .resetLibraryMetadata(MediaServerLibraryId(id), removeComicInfo)
            exitProcess(0)
        }
    }
}
