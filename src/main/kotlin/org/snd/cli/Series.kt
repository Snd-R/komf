package org.snd.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.metadata.model.ProviderSeriesId
import org.snd.module.CliModule
import kotlin.system.exitProcess

class Series : CliktCommand() {
    override fun run() {}

    class Search : CliktCommand() {
        private val module by requireObject<CliModule>()
        private val name by argument()

        override fun run() {
            val client = module.komgaModule.komgaClient
            val series = client.searchSeries(name = name, page = 0, pageSize = 10).content
                .joinToString("\n") { "${it.name} - ${it.id}" }
            echo(series)
            exitProcess(0)
        }
    }

    class Update : CliktCommand() {
        private val module by requireObject<CliModule>()
        private val id by argument()
        override fun run() {
            module.komgaModule.komgaMetadataService.matchSeriesMetadata(KomgaSeriesId(id))
            exitProcess(0)
        }
    }

    class Identify : CliktCommand() {
        private val module by requireObject<CliModule>()
        private val name by option().prompt()
        private val edition by option()
        private val id by argument()

        override fun run() {
            val series = module.komgaModule.komgaClient.getSeries(KomgaSeriesId(id))
            echo("searching...")
            val results = module.komgaModule.komgaMetadataService.searchSeriesMetadata(name)
            val output = results.mapIndexed { index, result -> "${index + 1}. ${result.title} - ${result.provider} id=${result.resultId}" }
                .joinToString("\n")
            echo(output)

            val resultIndex = prompt("choose a result number")?.toIntOrNull()?.let { it - 1 }
            if (resultIndex == null || resultIndex > results.size || resultIndex < 0) {
                echo("incorrect result number")
                exitProcess(1)
            }
            val selectedResult = results.elementAt(resultIndex)
            module.komgaModule.komgaMetadataService.setSeriesMetadata(
                series.seriesId(),
                selectedResult.provider,
                ProviderSeriesId(selectedResult.resultId),
                edition
            )

            exitProcess(0)
        }
    }

    class Reset : CliktCommand() {
        private val module by requireObject<CliModule>()
        private val id by argument()

        override fun run() {
            module.komgaModule.komgaMetadataService.resetSeriesMetadata(KomgaSeriesId(id))
            exitProcess(0)
        }
    }
}


