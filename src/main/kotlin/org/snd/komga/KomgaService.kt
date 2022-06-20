package org.snd.komga

import mu.KotlinLogging
import org.snd.config.MetadataUpdateConfig
import org.snd.komga.model.MatchedBook
import org.snd.komga.model.MatchedSeries
import org.snd.komga.model.dto.KomgaBook
import org.snd.komga.model.dto.KomgaBookId
import org.snd.komga.model.dto.KomgaLibraryId
import org.snd.komga.model.dto.KomgaSeries
import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.komga.model.dto.KomgaSeriesMetadataUpdate
import org.snd.komga.model.dto.KomgaThumbnailId
import org.snd.komga.repository.MatchedBookRepository
import org.snd.komga.repository.MatchedSeriesRepository
import org.snd.metadata.MetadataProvider
import org.snd.metadata.Provider
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.Thumbnail
import org.snd.metadata.model.VolumeMetadata

private val logger = KotlinLogging.logger {}

class KomgaService(
    private val komgaClient: KomgaClient,
    private val metadataProviders: Map<Provider, MetadataProvider>,
    private val matchedSeriesRepository: MatchedSeriesRepository,
    private val matchedBookRepository: MatchedBookRepository,
    private val metadataUpdateConfig: MetadataUpdateConfig,
    private val metadataUpdateMapper: MetadataUpdateMapper
) {
    fun availableProviders(): Set<Provider> = metadataProviders.keys

    fun searchSeriesMetadata(seriesName: String): Collection<SeriesSearchResult> {
        return metadataProviders.values.flatMap { it.searchSeries(seriesName) }
    }

    fun setSeriesMetadata(seriesId: KomgaSeriesId, providerName: Provider, providerSeriesId: ProviderSeriesId) {
        val provider = metadataProviders[providerName] ?: throw RuntimeException()
        val seriesMetadata = provider.getSeriesMetadata(providerSeriesId)
        val series = komgaClient.getSeries(seriesId)

        updateSeriesMetadata(series, seriesMetadata)
        overrideReadingDirection(series.seriesId())
    }

    fun matchLibraryMetadata(libraryId: KomgaLibraryId, provider: Provider? = null) {
        var page = 0
        do {
            val currentPage = komgaClient.getSeries(libraryId, false, page)
            currentPage.content.forEach { matchSeriesMetadata(it.seriesId(), provider) }
            page++
        } while (!currentPage.last)
    }

    fun matchSeriesMetadata(seriesId: KomgaSeriesId, provider: Provider? = null) {
        val series = komgaClient.getSeries(seriesId)
        if (provider != null) {
            val metadataProvider = metadataProviders[provider] ?: throw RuntimeException()
            metadataProvider.matchSeriesMetadata(series.name)?.let { meta -> updateSeriesMetadata(series, meta) }
        } else {
            metadataProviders.values
                .firstNotNullOfOrNull { it.matchSeriesMetadata(series.name) }
                ?.let { updateSeriesMetadata(series, it) }
                ?: run { logger.info { "no match found for series ${series.name} ${series.id}" } }
        }
        overrideReadingDirection(series.seriesId())
    }

    private fun updateSeriesMetadata(series: KomgaSeries, metadata: SeriesMetadata) {
        logger.info { "updating ${series.name} metadata to ${metadata.provider} ${metadata.id} ${metadata.title}" }

        val metadataUpdate = metadataUpdateMapper.toSeriesMetadataUpdate(metadata, series.metadata)
        komgaClient.updateSeriesMetadata(series.seriesId(), metadataUpdate)
        updateBookMetadata(series.seriesId(), metadata)

        val matchedSeries = matchedSeriesRepository.findFor(series.seriesId())
        val newThumbnail = if (metadataUpdateConfig.seriesThumbnails) metadata.thumbnail else null
        val thumbnailId = replaceSeriesThumbnail(series.seriesId(), newThumbnail, matchedSeries?.thumbnailId)

        val newMatch = MatchedSeries(
            seriesId = series.seriesId(),
            thumbnailId = thumbnailId,
            provider = metadata.provider,
            providerSeriesId = metadata.id,
        )
        if (matchedSeries == null) matchedSeriesRepository.insert(newMatch)
        else matchedSeriesRepository.update(newMatch)
    }

    private fun updateBookMetadata(seriesId: KomgaSeriesId, seriesMeta: SeriesMetadata) {
        val books = komgaClient.getBooks(seriesId, true).content

        matchBooksToMedata(books, seriesMeta.volumeMetadata).forEach { (book, volumeMeta) ->
            if (volumeMeta != null) {
                val metadataUpdate = metadataUpdateMapper.toBookMetadataUpdate(volumeMeta, book.metadata)
                komgaClient.updateBookMetadata(book.bookId(), metadataUpdate)
            } else {
                val metadataUpdate = metadataUpdateMapper.toBookMetadataUpdate(seriesMeta, book.metadata)
                komgaClient.updateBookMetadata(book.bookId(), metadataUpdate)
            }

            val matchedBook = matchedBookRepository.findFor(book.bookId())

            val newThumbnail = if (metadataUpdateConfig.bookThumbnails) volumeMeta?.thumbnail else null
            val thumbnailId = replaceBookThumbnail(book.bookId(), newThumbnail, matchedBook?.thumbnailId)

            val newMatch = MatchedBook(
                seriesId = seriesId,
                bookId = book.bookId(),
                thumbnailId = thumbnailId,
            )
            if (matchedBook == null) matchedBookRepository.insert(newMatch)
            else matchedBookRepository.update(newMatch)
        }
    }

    private fun replaceSeriesThumbnail(seriesId: KomgaSeriesId, thumbnail: Thumbnail?, oldThumbnail: KomgaThumbnailId?): KomgaThumbnailId? {
        val thumbnails = komgaClient.getSeriesThumbnails(seriesId)

        val thumbnailId = thumbnail?.let {
            komgaClient.uploadSeriesThumbnail(
                seriesId = seriesId,
                thumbnail = thumbnail,
                selected = thumbnails.isEmpty()
            )
        }

        oldThumbnail?.let { thumb ->
            if (thumbnails.any { it.id == thumb.id }) {
                komgaClient.deleteSeriesThumbnail(seriesId, thumb)
            }
        }

        return thumbnailId?.let { KomgaThumbnailId(it.id) }
    }

    private fun replaceBookThumbnail(bookId: KomgaBookId, thumbnail: Thumbnail?, oldThumbnail: KomgaThumbnailId?): KomgaThumbnailId? {
        val thumbnails = komgaClient.getBookThumbnails(bookId)

        val thumbnailId = thumbnail?.let {
            komgaClient.uploadBookThumbnail(
                bookId = bookId,
                thumbnail = thumbnail,
                selected = thumbnails.all { it.type == "GENERATED" || it.id == oldThumbnail?.id }
            )
        }

        oldThumbnail?.let { thumb ->
            if (thumbnails.any { it.id == thumb.id }) {
                komgaClient.deleteBookThumbnail(bookId, thumb)
            }
        }

        return thumbnailId?.let { KomgaThumbnailId(it.id) }
    }

    private fun matchBooksToMedata(books: Collection<KomgaBook>, metadata: Collection<VolumeMetadata>): Map<KomgaBook, VolumeMetadata?> {
        val nameRegex = ("(\\s\\(?[vtT](?<volume>[0-9]+)\\)?)").toRegex()

        return books.associateWith { book ->
            val matchedGroups = nameRegex.find(book.name)?.groups
            val volume = matchedGroups?.get("volume")?.value?.toIntOrNull()
            val meta = metadata.firstOrNull { meta -> meta.number != null && volume != null && meta.number == volume }

            meta
        }
    }

    private fun overrideReadingDirection(seriesId: KomgaSeriesId) {
        metadataUpdateConfig.readingDirectionValue?.let { readingDirection ->
            logger.info { "updating reading direction" }
            komgaClient.updateSeriesMetadata(seriesId, KomgaSeriesMetadataUpdate(readingDirection = readingDirection.toString()))
        }
    }
}
