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
import org.snd.metadata.BookFilenameParser
import org.snd.metadata.MetadataProvider
import org.snd.metadata.Provider
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.SeriesBook
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.Thumbnail

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
        val bookMetadata = getBookMetadata(seriesId, seriesMetadata, provider)

        updateSeriesMetadata(series, seriesMetadata)
        updateBookMetadata(bookMetadata, seriesMetadata)
        overrideReadingDirection(series.seriesId())
        logger.info { "updated metadata for \"${series.name}\" ${series.seriesId()}" }
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
        logger.info { "attempting to match series \"${series.name}\" ${series.seriesId()}" }
        val seriesMetadata = if (provider != null) {
            metadataProviders[provider]!!.matchSeriesMetadata(series.name)
        } else {
            metadataProviders.values.firstNotNullOfOrNull { it.matchSeriesMetadata(series.name) }
        }
        if (seriesMetadata == null) {
            logger.info { "no match found for series ${series.name} ${series.id}" }
            return
        }
        logger.info { "found match: \"${seriesMetadata.title}\" from ${seriesMetadata.provider}  ${seriesMetadata.id}" }

        val bookMetadata = getBookMetadata(series.seriesId(), seriesMetadata, metadataProviders[seriesMetadata.provider]!!)
        updateSeriesMetadata(series, seriesMetadata)
        updateBookMetadata(bookMetadata, seriesMetadata)
        overrideReadingDirection(series.seriesId())
        logger.info { "updated metadata for \"${series.name}\" ${series.seriesId()}" }
    }

    private fun updateSeriesMetadata(series: KomgaSeries, metadata: SeriesMetadata) {
        val metadataUpdate = metadataUpdateMapper.toSeriesMetadataUpdate(metadata, series.metadata)
        komgaClient.updateSeriesMetadata(series.seriesId(), metadataUpdate)

        val newThumbnail = if (metadataUpdateConfig.seriesThumbnails) metadata.thumbnail else null
        val thumbnailId = replaceSeriesThumbnail(series.seriesId(), newThumbnail)

        matchedSeriesRepository.save(
            MatchedSeries(
                seriesId = series.seriesId(),
                thumbnailId = thumbnailId,
                provider = metadata.provider,
                providerSeriesId = metadata.id,
            )
        )
    }

    private fun getBookMetadata(
        seriesId: KomgaSeriesId,
        seriesMeta: SeriesMetadata,
        provider: MetadataProvider,
    ): Map<KomgaBook, BookMetadata?> {
        val books = komgaClient.getBooks(seriesId, true).content
        val metadataMatch = associateBookMetadata(books, seriesMeta.books)
        if (metadataMatch.values.any { it != null }) logger.info { "fetching book data" }

        return metadataMatch.map { (book, seriesBookMeta) ->
            if (seriesBookMeta != null) {
                book to provider.getBookMetadata(seriesMeta.id, seriesBookMeta.id)
            } else {
                book to null
            }
        }.toMap()
    }

    private fun updateBookMetadata(bookMetadata: Map<KomgaBook, BookMetadata?>, seriesMetadata: SeriesMetadata) {
        bookMetadata.forEach { (book, metadata) -> updateBookMetadata(book, metadata, seriesMetadata) }
    }

    private fun updateBookMetadata(book: KomgaBook, metadata: BookMetadata?, seriesMeta: SeriesMetadata) {
        val metadataUpdate = if (metadata == null) metadataUpdateMapper.toBookMetadataUpdate(seriesMeta, book.metadata)
        else metadataUpdateMapper.toBookMetadataUpdate(metadata, book.metadata)

        komgaClient.updateBookMetadata(book.bookId(), metadataUpdate)

        val newThumbnail = if (metadataUpdateConfig.bookThumbnails) metadata?.thumbnail else null
        val thumbnailId = replaceBookThumbnail(book.bookId(), newThumbnail)

        matchedBookRepository.save(
            MatchedBook(
                seriesId = book.seriesId(),
                bookId = book.bookId(),
                thumbnailId = thumbnailId,
            )
        )
    }

    private fun replaceSeriesThumbnail(seriesId: KomgaSeriesId, thumbnail: Thumbnail?): KomgaThumbnailId? {
        val matchedSeries = matchedSeriesRepository.findFor(seriesId)
        val thumbnails = komgaClient.getSeriesThumbnails(seriesId)

        val thumbnailId = thumbnail?.let {
            komgaClient.uploadSeriesThumbnail(
                seriesId = seriesId,
                thumbnail = thumbnail,
                selected = thumbnails.isEmpty()
            )
        }

        matchedSeries?.thumbnailId?.let { thumb ->
            if (thumbnails.any { it.id == thumb.id }) {
                komgaClient.deleteSeriesThumbnail(seriesId, thumb)
            }
        }

        return thumbnailId?.let { KomgaThumbnailId(it.id) }
    }

    private fun replaceBookThumbnail(bookId: KomgaBookId, thumbnail: Thumbnail?): KomgaThumbnailId? {
        val existingMatch = matchedBookRepository.findFor(bookId)
        val thumbnails = komgaClient.getBookThumbnails(bookId)

        val thumbnailId = thumbnail?.let {
            komgaClient.uploadBookThumbnail(
                bookId = bookId,
                thumbnail = thumbnail,
                selected = thumbnails.all { it.type == "GENERATED" || it.id == existingMatch?.thumbnailId?.id }
            )
        }

        existingMatch?.thumbnailId?.let { thumb ->
            if (thumbnails.any { it.id == thumb.id }) {
                komgaClient.deleteBookThumbnail(bookId, thumb)
            }
        }

        return thumbnailId?.let { KomgaThumbnailId(it.id) }
    }

    private fun overrideReadingDirection(seriesId: KomgaSeriesId) {
        metadataUpdateConfig.readingDirectionValue?.let { readingDirection ->
            logger.info { "updating reading direction" }
            komgaClient.updateSeriesMetadata(seriesId, KomgaSeriesMetadataUpdate(readingDirection = readingDirection.toString()))
        }
    }

    private fun associateBookMetadata(books: Collection<KomgaBook>, providerBooks: Collection<SeriesBook>): Map<KomgaBook, SeriesBook?> {
        val editions = providerBooks.groupBy { it.edition }
        val noEditionBooks = providerBooks.filter { it.edition == null }

        val byEdition: Map<KomgaBook, String?> = books.associateWith { book ->
            val bookExtraData = BookFilenameParser.getExtraData(book.name).map { it.lowercase() }
            editions.keys.firstOrNull { bookExtraData.contains(it) }
        }

        return byEdition.map { (book, edition) ->
            val volume = BookFilenameParser.getVolumes(book.name)
            val matched = if (volume == null || volume.first != volume.last) {
                null
            } else if (edition == null) {
                noEditionBooks.firstOrNull { it.number == volume.first }
            } else
                editions[edition]?.firstOrNull { it.number == volume.first }
            book to matched
        }.toMap()
    }
}
