package org.snd.komga

import mu.KotlinLogging
import org.snd.komga.model.*
import org.snd.metadata.MetadataProvider
import org.snd.metadata.Provider
import org.snd.metadata.ProviderSeriesId
import org.snd.model.SeriesMetadata
import org.snd.model.SeriesSearchResult

private val logger = KotlinLogging.logger {}

class KomgaService(
    private val komgaClient: KomgaClient,
    private val metadataProviders: Map<Provider, MetadataProvider>
) {

    fun setSeriesMetadata(seriesId: SeriesId, providerName: Provider, providerSeriesId: ProviderSeriesId) {
        val provider = metadataProviders[providerName] ?: throw RuntimeException()
        val seriesMetadata = provider.getSeriesMetadata(providerSeriesId)

        updateSeriesMetadata(seriesId, seriesMetadata)
    }

    fun searchSeriesMetadata(seriesName: String): Collection<SeriesSearchResult> {
        return metadataProviders.values.flatMap { it.searchSeries(seriesName) }
    }

    fun matchSeriesMetadata(seriesId: SeriesId) {
        logger.info { "attempting to match series $seriesId" }
        val series = komgaClient.getSeries(seriesId)
        metadataProviders.values
            .firstNotNullOfOrNull { it.matchSeriesMetadata(series.name) }
            ?.let {
                logger.info { "matched series $seriesId to ${it.title}" }
                updateSeriesMetadata(seriesId, it)
            }
    }

    fun matchBookMetadata(seriesId: SeriesId, bookId: BookId) {
        val series = komgaClient.getSeries(seriesId)

        metadataProviders.values
            .firstNotNullOfOrNull { it.matchSeriesMetadata(series.name) }
            ?.let { matchBookMetadata(bookId, it) }
    }

    fun matchLibraryMetadata(libraryId: LibraryId) {
        do {
            val page = komgaClient.getSeries(libraryId, false)
            page.content.forEach { matchSeriesMetadata(it.seriesId()) }
        } while (!page.last)
    }

    fun availableProviders(): Set<Provider> = metadataProviders.keys

    private fun updateSeriesMetadata(seriesId: SeriesId, metadata: SeriesMetadata) {
        komgaClient.updateSeriesMetadata(seriesId, metadata.toSeriesUpdate())
        komgaClient.getBooks(seriesId, true).content.forEach {
            komgaClient.updateBookMetadataAsync(it.bookId(), metadata.toBookMetadataUpdate())
        }

        metadata.thumbnail?.let { komgaClient.updateSeriesThumbnail(seriesId, it.thumbnail) }
    }

    private fun matchBookMetadata(bookId: BookId, metadata: SeriesMetadata) {
        komgaClient.updateBookMetadata(bookId, metadata.toBookMetadataUpdate())
    }
}
