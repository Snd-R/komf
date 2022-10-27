package org.snd.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import org.snd.mediaserver.model.MediaServerLibraryId
import org.snd.module.CliModule
import kotlin.system.exitProcess

class Library : CliktCommand() {
    override fun run() {}

    class Update : CliktCommand() {
        private val module by requireObject<CliModule>()
        private val id by argument()
        override fun run() {
            module.mediaServerModule.komgaMetadataService.matchLibraryMetadata(MediaServerLibraryId(id))
            exitProcess(0)
        }
    }

    class Reset : CliktCommand() {
        private val module by requireObject<CliModule>()
        private val id by argument()
        override fun run() {
            module.mediaServerModule.komgaMetadataService.resetLibraryMetadata(MediaServerLibraryId(id))
            exitProcess(0)
        }
    }
}
