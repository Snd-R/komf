package org.snd.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.metadata.model.ProviderSeriesId
import org.snd.module.context.CliAppContext
import kotlin.system.exitProcess

class Series : CliktCommand() {
    override fun run() {}

    class Search : CliktCommand() {
        private val context by requireObject<CliAppContext>()
        private val name by argument()

        override fun run() {
            val client = context.cliAppModule.mediaServerClient
            val series = client.searchSeries(name = name)
                .joinToString("\n") { "${it.name} - ${it.id}" }
            echo(series)
            exitProcess(0)
        }
    }

    class Update : CliktCommand() {
        private val context by requireObject<CliAppContext>()
        private val id by argument()
        override fun run() {
            val client = context.cliAppModule.mediaServerClient
            val series = client.getSeries(MediaServerSeriesId(id))
            context.cliAppModule.metadataServiceProvider.serviceFor(series.libraryId.id)
                .matchSeriesMetadata(MediaServerSeriesId(id))
            exitProcess(0)
        }
    }

    class Identify : CliktCommand() {
        private val context by requireObject<CliAppContext>()
        private val name by option().prompt()
        private val edition by option()
        private val id by argument()

        override fun run() {
            val client = context.cliAppModule.mediaServerClient
            val series = client.getSeries(MediaServerSeriesId(id))
            val komgaMetadataService = context.cliAppModule.metadataServiceProvider.serviceFor(series.libraryId.id)
            echo("searching...")
            val results = komgaMetadataService.searchSeriesMetadata(name, series.libraryId)
            val output = results.mapIndexed { index, result -> "${index + 1}. ${result.title} - ${result.provider} id=${result.resultId}" }
                .joinToString("\n")
            echo(output)

            val resultIndex = prompt("choose a result number")?.toIntOrNull()?.let { it - 1 }
            if (resultIndex == null || resultIndex > results.size || resultIndex < 0) {
                echo("incorrect result number")
                exitProcess(1)
            }
            val selectedResult = results.elementAt(resultIndex)
            komgaMetadataService.setSeriesMetadata(
                series.id,
                selectedResult.provider,
                ProviderSeriesId(selectedResult.resultId),
                edition
            )

            exitProcess(0)
        }
    }

    class Reset : CliktCommand() {
        private val context by requireObject<CliAppContext>()
        private val id by argument()

        override fun run() {
            val client = context.cliAppModule.mediaServerClient
            val series = client.getSeries(MediaServerSeriesId(id))
            context.cliAppModule.metadataUpdateServiceProvider.serviceFor(series.libraryId.id)
                .resetSeriesMetadata(MediaServerSeriesId(id))
            exitProcess(0)
        }
    }
}


