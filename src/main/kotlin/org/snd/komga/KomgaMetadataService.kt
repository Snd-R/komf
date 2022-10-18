package org.snd.komga

import jakarta.xml.bind.ValidationException
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.snd.config.MetadataUpdateConfig
import org.snd.komga.UpdateMode.API
import org.snd.komga.UpdateMode.FILE_EMBED
import org.snd.komga.model.MatchedBook
import org.snd.komga.model.MatchedSeries
import org.snd.komga.model.SeriesAndBookMetadata
import org.snd.komga.model.dto.KomgaBook
import org.snd.komga.model.dto.KomgaBookId
import org.snd.komga.model.dto.KomgaBookMetadataUpdate
import org.snd.komga.model.dto.KomgaLibraryId
import org.snd.komga.model.dto.KomgaSeries
import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.komga.model.dto.KomgaSeriesMetadataUpdate
import org.snd.komga.model.dto.KomgaThumbnailId
import org.snd.komga.repository.MatchedBookRepository
import org.snd.komga.repository.MatchedSeriesRepository
import org.snd.metadata.BookFilenameParser
import org.snd.metadata.MetadataMerger
import org.snd.metadata.MetadataProvider
import org.snd.metadata.comicinfo.ComicInfoWriter
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.Image
import org.snd.metadata.model.Provider
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesBook
import org.snd.metadata.model.SeriesMatchResult
import org.snd.metadata.model.SeriesMatchStatus.*
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesMetadata.Status.ONGOING
import org.snd.metadata.model.SeriesSearchResult
import java.nio.file.Path
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.ExecutorService

private val logger = KotlinLogging.logger {}

class KomgaMetadataService(
    private val komgaClient: KomgaClient,
    private val metadataProviders: Map<Provider, MetadataProvider>,
    private val matchedSeriesRepository: MatchedSeriesRepository,
    private val matchedBookRepository: MatchedBookRepository,
    private val metadataUpdateConfig: MetadataUpdateConfig,
    private val metadataUpdateMapper: MetadataUpdateMapper,
    private val aggregateMetadata: Boolean,
    private val executor: ExecutorService,
    private val comicInfoWriter: ComicInfoWriter,
) {
    fun availableProviders(): Set<Provider> = metadataProviders.keys

    fun searchSeriesMetadata(seriesName: String): Collection<SeriesSearchResult> {
        return metadataProviders.values.flatMap { it.searchSeries(seriesName) }
    }

    fun setSeriesMetadata(
        seriesId: KomgaSeriesId,
        providerName: Provider,
        providerSeriesId: ProviderSeriesId,
        edition: String?
    ) {
        val provider = metadataProviders[providerName] ?: throw RuntimeException()
        val series = komgaClient.getSeries(seriesId)

        val seriesMetadata = provider.getSeriesMetadata(providerSeriesId)
        val bookMetadata = getBookMetadata(seriesId, seriesMetadata, provider, edition)

        val metadata = if (aggregateMetadata) {
            aggregateMetadataFromProviders(
                series,
                SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata),
                metadataProviders.values.filter { it != provider },
                edition
            )
        } else SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata)

        if (metadata.seriesMetadata != null) updateSeriesMetadata(series, metadata.seriesMetadata)
        updateBookMetadata(metadata.bookMetadata, metadata.seriesMetadata)

        if (metadataUpdateConfig.mode == FILE_EMBED) {
            komgaClient.analyzeSeries(series.seriesId())
        }
    }

    fun matchLibraryMetadata(libraryId: KomgaLibraryId) {
        var errorCount = 0
        generateSequence(komgaClient.getSeries(libraryId, false, 0)) {
            if (it.last) null
            else komgaClient.getSeries(libraryId, false, it.number + 1)
        }
            .flatMap { it.content }
            .forEach {
                runCatching {
                    matchSeriesMetadata(it.seriesId())
                }.onFailure {
                    logger.error(it) { }
                    errorCount += 1
                }
            }

        logger.info { "Finished library scan. Encountered $errorCount errors" }
    }

    fun matchSeriesMetadata(seriesId: KomgaSeriesId) {
        val series = komgaClient.getSeries(seriesId)
        logger.info { "attempting to match series \"${series.name}\" ${series.seriesId()}" }
        val matchResult = metadataProviders.values.asSequence()
            .map { it to it.matchSeriesMetadata(series.name) }
            .mapNotNull { (provider, match) ->
                val metadata = when (match.status) {
                    MATCHED -> handleSingleMatch(series, provider, match, null)
                    MULTIPLE_MATCHES -> handleMultipleMatches(series, provider, match, null)
                    NO_MATCH -> null
                }
                if (metadata == null) null
                else provider to metadata
            }
            .firstOrNull()

        if (matchResult == null) {
            logger.info { "no match found for series ${series.name} ${series.id}" }
            return
        }

        val metadata = matchResult.let { (provider, metadata) ->
            if (aggregateMetadata) {
                aggregateMetadataFromProviders(
                    series,
                    metadata,
                    metadataProviders.values.filter { it != provider },
                    null
                )
            } else metadata
        }

        if (metadata.seriesMetadata != null) updateSeriesMetadata(series, metadata.seriesMetadata)
        updateBookMetadata(metadata.bookMetadata, metadata.seriesMetadata)

        if (metadataUpdateConfig.mode == FILE_EMBED) {
            komgaClient.analyzeSeries(series.seriesId())
        }
    }

    fun resetSeriesMetadata(seriesId: KomgaSeriesId) {
        val series = komgaClient.getSeries(seriesId)
        resetSeriesMetadata(series)
    }

    private fun resetSeriesMetadata(series: KomgaSeries) {
        komgaClient.updateSeriesMetadata(
            series.seriesId(),
            KomgaSeriesMetadataUpdate(
                status = ONGOING.name,
                title = series.name,
                titleSort = series.name,
                summary = "",
                publisher = "",
                readingDirection = null,
                ageRating = null,
                language = "",
                genres = emptyList(),
                tags = emptyList(),
                totalBookCount = null,
                statusLock = false,
                titleLock = false,
                titleSortLock = false,
                summaryLock = false,
                publisherLock = false,
                readingDirectionLock = false,
                ageRatingLock = false,
                languageLock = false,
                genresLock = false,
                tagsLock = false,
                totalBookCountLock = false
            )
        )

        komgaClient.getBooks(series.seriesId(), true)
            .content.forEach { resetBookMetadata(it) }

        replaceSeriesThumbnail(series.seriesId(), null)
        matchedSeriesRepository.delete(series.seriesId())
    }

    private fun resetBookMetadata(book: KomgaBook) {
        komgaClient.updateBookMetadata(
            book.bookId(),
            KomgaBookMetadataUpdate(
                title = book.name,
                summary = "",
                releaseDate = null,
                authors = emptyList(),
                tags = emptySet(),
                isbn = null,
                links = emptyList(),

                titleLock = false,
                summaryLock = false,
                numberLock = false,
                numberSortLock = false,
                releaseDateLock = false,
                authorsLock = false,
                tagsLock = false,
                isbnLock = false,
                linksLock = false

            )
        )

        replaceBookThumbnail(book.bookId(), null)
        matchedBookRepository.delete(book.bookId())
    }

    fun resetLibraryMetadata(libraryId: KomgaLibraryId) {
        generateSequence(komgaClient.getSeries(libraryId, false, 0)) {
            if (it.last) null
            else komgaClient.getSeries(libraryId, false, it.number + 1)
        }
            .flatMap { it.content }
            .forEach { resetSeriesMetadata(it) }
    }

    private fun updateSeriesMetadata(series: KomgaSeries, metadata: SeriesMetadata) {
        if (metadataUpdateConfig.mode == API) {
            val metadataUpdate = metadataUpdateMapper.toSeriesMetadataUpdate(metadata, series.metadata)
            komgaClient.updateSeriesMetadata(series.seriesId(), metadataUpdate)
        }

        val newThumbnail = if (metadataUpdateConfig.seriesThumbnails) metadata.thumbnail else null
        val thumbnailId = replaceSeriesThumbnail(series.seriesId(), newThumbnail)

        if (thumbnailId == null) {
            matchedSeriesRepository.delete(series.seriesId())
        } else {
            matchedSeriesRepository.save(
                MatchedSeries(
                    seriesId = series.seriesId(),
                    thumbnailId = thumbnailId,
                )
            )
        }
    }

    private fun getBookMetadata(
        seriesId: KomgaSeriesId,
        seriesMeta: ProviderSeriesMetadata,
        provider: MetadataProvider,
        bookEdition: String?
    ): Map<KomgaBook, BookMetadata?> {
        val books = komgaClient.getBooks(seriesId, true).content
        val metadataMatch = associateBookMetadata(books, seriesMeta.books, bookEdition)

        return metadataMatch.map { (book, seriesBookMeta) ->
            if (seriesBookMeta != null) {
                book to provider.getBookMetadata(seriesMeta.id, seriesBookMeta.id).metadata
            } else {
                book to null
            }
        }.toMap()
    }

    private fun updateBookMetadata(bookMetadata: Map<KomgaBook, BookMetadata?>, seriesMetadata: SeriesMetadata?) {
        bookMetadata.forEach { (book, metadata) -> updateBookMetadata(book, metadata, seriesMetadata) }
    }

    private fun updateBookMetadata(book: KomgaBook, metadata: BookMetadata?, seriesMeta: SeriesMetadata?) {
        if (metadataUpdateConfig.mode == API) {
            metadataUpdateMapper.toBookMetadataUpdate(metadata, seriesMeta, book.metadata)
                ?.let { komgaClient.updateBookMetadata(book.bookId(), it) }
        } else if (book.deleted.not()) {
            metadataUpdateMapper.toComicInfo(metadata, seriesMeta)?.let {
                comicInfoWriter.writeMetadata(Path.of(book.url), it)
            }
        }

        val newThumbnail = if (metadataUpdateConfig.bookThumbnails) metadata?.thumbnail else null
        val thumbnailId = replaceBookThumbnail(book.bookId(), newThumbnail)

        if (thumbnailId == null) {
            matchedBookRepository.delete(book.bookId())
        } else {
            matchedBookRepository.save(
                MatchedBook(
                    seriesId = book.seriesId(),
                    bookId = book.bookId(),
                    thumbnailId = thumbnailId,
                )
            )
        }
    }

    private fun replaceSeriesThumbnail(seriesId: KomgaSeriesId, thumbnail: Image?): KomgaThumbnailId? {
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

    private fun replaceBookThumbnail(bookId: KomgaBookId, thumbnail: Image?): KomgaThumbnailId? {
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

    private fun associateBookMetadata(
        books: Collection<KomgaBook>,
        providerBooks: Collection<SeriesBook>,
        edition: String? = null
    ): Map<KomgaBook, SeriesBook?> {
        val editions = providerBooks.groupBy { it.edition }
        val noEditionBooks = providerBooks.filter { it.edition == null }

        if (edition != null) {
            val editionName = edition.replace("(?i)\\s?[EÉ]dition\\s?".toRegex(), "").lowercase()
            return books.associateWith { komgaBook ->
                val volume = BookFilenameParser.getVolumes(komgaBook.name)?.first ?: komgaBook.number
                editions[editionName]?.firstOrNull { volume == it.number }
            }
        }

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
            } else {
                editions[edition]?.firstOrNull { it.number == volume.first }
            }
            book to matched
        }.toMap()
    }

    private fun aggregateMetadataFromProviders(
        series: KomgaSeries,
        metadata: SeriesAndBookMetadata,
        providers: Collection<MetadataProvider>,
        edition: String?
    ): SeriesAndBookMetadata {
        if (providers.isEmpty()) return metadata

        val searchTitles = setOfNotNull(series.name, metadata.seriesMetadata?.title) + (metadata.seriesMetadata?.alternativeTitles ?: emptySet())

        return providers.map { provider -> supplyAsync({ getAggregationMetadata(series, searchTitles, provider, edition) }, executor) }
            .mapNotNull { it.join() }
            .fold(metadata) { oldMetadata, newMetadata -> mergeMetadata(oldMetadata, newMetadata) }
    }

    private fun getAggregationMetadata(
        series: KomgaSeries,
        searchTitles: Collection<String>,
        provider: MetadataProvider,
        bookEdition: String?
    ): SeriesAndBookMetadata? {
        return searchTitles.asSequence()
            .filter { StringUtils.isAsciiPrintable(it) }
            .onEach { logger.info { "searching \"$it\" using ${provider.providerName()}" } }
            .map { provider.matchSeriesMetadata(it) }
            .mapNotNull {
                when (it.status) {
                    MATCHED -> handleSingleMatch(series, provider, it, bookEdition)
                    MULTIPLE_MATCHES -> handleMultipleMatches(series, provider, it, bookEdition)
                    NO_MATCH -> null
                }
            }
            .firstOrNull()
    }

    private fun mergeMetadata(originalMetadata: SeriesAndBookMetadata, newMetadata: SeriesAndBookMetadata): SeriesAndBookMetadata {
        val mergedSeries = if (originalMetadata.seriesMetadata != null && newMetadata.seriesMetadata != null) {
            MetadataMerger.mergeSeriesMetadata(originalMetadata.seriesMetadata, newMetadata.seriesMetadata)
        } else originalMetadata.seriesMetadata ?: newMetadata.seriesMetadata

        val books = originalMetadata.bookMetadata.keys.associateBy { it.id }
        val mergedBooks = MetadataMerger.mergeBookMetadata(
            originalMetadata.bookMetadata.map { it.key.id to it.value }.toMap(),
            newMetadata.bookMetadata.map { it.key.id to it.value }.toMap()
        ).map { (bookId, metadata) -> books[bookId]!! to metadata }.toMap()

        return SeriesAndBookMetadata(mergedSeries, mergedBooks)
    }

    private fun handleMultipleMatches(series: KomgaSeries, provider: MetadataProvider, match: SeriesMatchResult, bookEdition: String?): SeriesAndBookMetadata {
        return SeriesAndBookMetadata(null, emptyMap())
    }

    private fun handleSingleMatch(series: KomgaSeries, provider: MetadataProvider, match: SeriesMatchResult, bookEdition: String?): SeriesAndBookMetadata {
        if (match.status != MATCHED) throw ValidationException("incorrect match type")
        val seriesMetadata = match.result!!.metadata
        logger.info { "found match: \"${seriesMetadata.title}\" from ${provider.providerName()}  ${match.result.id}" }

        val bookMetadata = getBookMetadata(series.seriesId(), match.result, provider, bookEdition)
        return SeriesAndBookMetadata(seriesMetadata, bookMetadata)
    }
}
