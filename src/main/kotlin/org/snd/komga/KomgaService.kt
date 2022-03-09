package org.snd.komga

import mu.KotlinLogging
import org.snd.komga.model.*
import org.snd.komga.repository.MatchedSeriesRepository
import org.snd.metadata.MetadataProvider
import org.snd.metadata.Provider
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.Thumbnail

private val logger = KotlinLogging.logger {}

class KomgaService(
    private val komgaClient: KomgaClient,
    private val metadataProviders: Map<Provider, MetadataProvider>,
    private val matchedSeriesRepository: MatchedSeriesRepository
) {

    fun setSeriesMetadata(seriesId: SeriesId, providerName: Provider, providerSeriesId: ProviderSeriesId) {
        val provider = metadataProviders[providerName] ?: throw RuntimeException()
        val seriesMetadata = provider.getSeriesMetadata(providerSeriesId)

        updateSeriesMetadata(seriesId, seriesMetadata)
    }

    fun searchSeriesMetadata(seriesName: String): Collection<SeriesSearchResult> {
        return metadataProviders.values.flatMap { it.searchSeries(seriesName) }
    }

    fun matchSeriesMetadata(seriesId: SeriesId, provider: Provider? = null) {
        if (provider != null) {
            val series = komgaClient.getSeries(seriesId)
            logger.info { "attempting to match series ${series.name} ${series.id}" }
            val metadataProvider = metadataProviders[provider] ?: throw RuntimeException()
            metadataProvider.matchSeriesMetadata(series.name)?.let { meta -> updateSeriesMetadata(seriesId, meta) }
            return
        }

        matchedSeriesRepository.findFor(seriesId)?.let { series ->
            metadataProviders[series.provider]?.let { provider ->
                val metadata = provider.getSeriesMetadata(series.providerSeriesId)
                updateSeriesMetadata(seriesId, metadata)
                return
            } ?: logger.warn { "can't find previously used metadata provider for series $seriesId" }
        }

        val series = komgaClient.getSeries(seriesId)
        logger.info { "attempting to match series ${series.name} ${series.id}" }
        metadataProviders.values
            .firstNotNullOfOrNull { it.matchSeriesMetadata(series.name) }
            ?.let {
                updateSeriesMetadata(seriesId, it)
            }
    }

    fun matchBookMetadata(seriesId: SeriesId, bookId: BookId) {
        val series = komgaClient.getSeries(seriesId)

        metadataProviders.values
            .firstNotNullOfOrNull { it.matchSeriesMetadata(series.name) }
            ?.let { matchBookMetadata(bookId, it) }
    }

    fun matchLibraryMetadata(libraryId: LibraryId, provider: Provider? = null) {
        var page = 0
        do {
            val currentPage = komgaClient.getSeries(libraryId, false, page)
            currentPage.content.forEach { matchSeriesMetadata(it.seriesId(), provider) }
            page++
        } while (!currentPage.last)
    }

    fun availableProviders(): Set<Provider> = metadataProviders.keys

    private fun updateSeriesMetadata(seriesId: SeriesId, metadata: SeriesMetadata) {
        logger.info { "updating $seriesId metadata to ${metadata.provider} ${metadata.id} ${metadata.title}" }
        komgaClient.updateSeriesMetadata(seriesId, metadata.toSeriesUpdate())
        komgaClient.getBooks(seriesId, true).content.forEach {
            komgaClient.updateBookMetadataAsync(it.bookId(), metadata.toBookMetadataUpdate())
        }
        val matchedSeries = matchedSeriesRepository.findFor(seriesId)
        val thumbnailId = replaceThumbnail(seriesId, metadata.thumbnail, matchedSeries?.thumbnailId)

        if (matchedSeries == null) {
            matchedSeriesRepository.insert(
                MatchedSeries(
                    seriesId = seriesId,
                    thumbnailId = thumbnailId,
                    provider = metadata.provider,
                    providerSeriesId = metadata.id
                )
            )
        } else {
            matchedSeriesRepository.update(
                matchedSeries.copy(
                    provider = metadata.provider,
                    providerSeriesId = metadata.id,
                    thumbnailId = thumbnailId
                )
            )
        }
    }

    private fun matchBookMetadata(bookId: BookId, metadata: SeriesMetadata) {
        komgaClient.updateBookMetadata(bookId, metadata.toBookMetadataUpdate())
    }

    private fun replaceThumbnail(
        seriesId: SeriesId,
        thumbnail: Thumbnail?,
        oldThumbnail: ThumbnailId?
    ): ThumbnailId? {
        val thumbnails = komgaClient.getSeriesThumbnails(seriesId)

        //FIXME workaround to get uploaded thumbnail id
        val thumbnailId = thumbnail?.let {
            komgaClient.uploadSeriesThumbnail(
                seriesId = seriesId,
                thumbnail = thumbnail,
                selected = thumbnails.isEmpty()
            )

            komgaClient.getSeriesThumbnails(seriesId).minus(thumbnails.toSet()).first().id
        }

        oldThumbnail?.let { thumb ->
            if (thumbnails.any { it.id == thumb.id }) {
                komgaClient.deleteSeriesThumbnail(seriesId, thumb)
            }
        }

        return thumbnailId?.let { ThumbnailId(it) }
    }
}
