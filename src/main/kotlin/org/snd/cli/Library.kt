package org.snd.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import org.snd.mediaserver.model.MediaServerLibraryId
import org.snd.module.CliContext
import kotlin.system.exitProcess

class Library : CliktCommand() {
    override fun run() {}

    class Update : CliktCommand() {
        private val context by requireObject<CliContext>()
        private val id by argument()
        override fun run() {
            context.cliModule.metadataService.matchLibraryMetadata(MediaServerLibraryId(id))
            exitProcess(0)
        }
    }

    class Reset : CliktCommand() {
        private val context by requireObject<CliContext>()
        private val id by argument()
        override fun run() {
            context.cliModule.metadataUpdateService.resetLibraryMetadata(MediaServerLibraryId(id))
            exitProcess(0)
        }
    }
}
