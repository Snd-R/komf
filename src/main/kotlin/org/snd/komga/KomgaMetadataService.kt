package org.snd.komga

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.snd.config.MetadataUpdateConfig
import org.snd.komga.UpdateMode.API
import org.snd.komga.UpdateMode.FILE_EMBED
import org.snd.komga.model.MatchedBook
import org.snd.komga.model.MatchedSeries
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
import org.snd.metadata.model.Provider
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesBook
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesMetadata.Status.ONGOING
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.Thumbnail
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

        val nonAggregatedSeriesMetadata = provider.getSeriesMetadata(providerSeriesId)
        val nonAggregatedBookMetadata = getBookMetadata(seriesId, nonAggregatedSeriesMetadata, provider, edition)

        val (seriesMetadata, bookMetadata) = if (aggregateMetadata) {
            aggregateMetadataFromProviders(
                series,
                nonAggregatedSeriesMetadata.metadata,
                nonAggregatedBookMetadata,
                metadataProviders.values.filter { it != provider },
                edition
            )
        } else nonAggregatedSeriesMetadata.metadata to nonAggregatedBookMetadata

        updateSeriesMetadata(series, seriesMetadata)
        updateBookMetadata(bookMetadata, seriesMetadata)

        if (metadataUpdateConfig.mode == FILE_EMBED) {
            komgaClient.analyzeSeries(series.seriesId())
        }
    }

    fun matchLibraryMetadata(libraryId: KomgaLibraryId) {
        generateSequence(komgaClient.getSeries(libraryId, false, 0)) {
            if (it.last) null
            else komgaClient.getSeries(libraryId, false, it.number + 1)
        }
            .flatMap { it.content }
            .forEach { matchSeriesMetadata(it.seriesId()) }
    }

    fun matchSeriesMetadata(seriesId: KomgaSeriesId) {
        val series = komgaClient.getSeries(seriesId)
        logger.info { "attempting to match series \"${series.name}\" ${series.seriesId()}" }
        val matchResult = metadataProviders.values.firstNotNullOfOrNull {
            val metadata = it.matchSeriesMetadata(series.name)
            if (metadata != null) it to metadata
            else null
        }

        if (matchResult == null) {
            logger.info { "no match found for series ${series.name} ${series.id}" }
            return
        }
        val (provider, providerSeriesMetadata) = matchResult
        logger.info { "found match: \"${providerSeriesMetadata.metadata.title}\" from ${providerSeriesMetadata.provider}  ${providerSeriesMetadata.id}" }

        val nonAggregatedBookMetadata = getBookMetadata(seriesId, providerSeriesMetadata, provider, null)
        val (seriesMetadata, bookMetadata) = if (aggregateMetadata) {
            aggregateMetadataFromProviders(
                series,
                providerSeriesMetadata.metadata,
                nonAggregatedBookMetadata,
                metadataProviders.values.filter { it != provider },
                null
            )
        } else providerSeriesMetadata.metadata to nonAggregatedBookMetadata

        updateSeriesMetadata(series, seriesMetadata)
        updateBookMetadata(bookMetadata, seriesMetadata)

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

        matchedSeriesRepository.save(
            MatchedSeries(
                seriesId = series.seriesId(),
                thumbnailId = thumbnailId,
            )
        )
    }

    private fun getBookMetadata(
        seriesId: KomgaSeriesId,
        seriesMeta: ProviderSeriesMetadata,
        provider: MetadataProvider,
        edition: String?
    ): Map<KomgaBook, BookMetadata?> {
        val books = komgaClient.getBooks(seriesId, true).content
        val metadataMatch = associateBookMetadata(books, seriesMeta.books, edition)

        return metadataMatch.map { (book, seriesBookMeta) ->
            if (seriesBookMeta != null) {
                book to provider.getBookMetadata(seriesMeta.id, seriesBookMeta.id).metadata
            } else {
                book to null
            }
        }.toMap()
    }

    private fun updateBookMetadata(bookMetadata: Map<KomgaBook, BookMetadata?>, seriesMetadata: SeriesMetadata) {
        bookMetadata.forEach { (book, metadata) -> updateBookMetadata(book, metadata, seriesMetadata) }
    }

    private fun updateBookMetadata(book: KomgaBook, metadata: BookMetadata?, seriesMeta: SeriesMetadata) {
        if (metadataUpdateConfig.mode == API) {
            val metadataUpdate = metadataUpdateMapper.toBookMetadataUpdate(metadata, seriesMeta, book.metadata)
            komgaClient.updateBookMetadata(book.bookId(), metadataUpdate)
        } else {
            val comicInfo = metadataUpdateMapper.toComicInfo(metadata, seriesMeta)
            comicInfoWriter.writeMetadata(Path.of(book.url), comicInfo)
        }

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

    private fun associateBookMetadata(
        books: Collection<KomgaBook>,
        providerBooks: Collection<SeriesBook>,
        edition: String? = null
    ): Map<KomgaBook, SeriesBook?> {
        val editions = providerBooks.groupBy { it.edition }
        val noEditionBooks = providerBooks.filter { it.edition == null }

        if (edition != null) {
            val editionName = edition.replace("\\s?[EÉ]dition\\s?".toRegex(), "").lowercase()
            return books.associateWith { komgaBook ->
                editions[editionName]?.firstOrNull { komgaBook.number == it.number }
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
            } else
                editions[edition]?.firstOrNull { it.number == volume.first }
            book to matched
        }.toMap()
    }

    private fun aggregateMetadataFromProviders(
        series: KomgaSeries,
        originalSeriesMetadata: SeriesMetadata,
        originalBookMetadata: Map<KomgaBook, BookMetadata?>,
        providers: Collection<MetadataProvider>,
        edition: String?
    ): Pair<SeriesMetadata, Map<KomgaBook, BookMetadata?>> {
        if (providers.isEmpty()) return originalSeriesMetadata to originalBookMetadata

        val searchTitles = setOfNotNull(series.name, originalSeriesMetadata.title) + originalSeriesMetadata.alternativeTitles

        return providers.map { provider -> supplyAsync({ getAggregationMetadata(series.seriesId(), searchTitles, provider, edition) }, executor) }
            .mapNotNull { it.join() }
            .fold(originalSeriesMetadata to originalBookMetadata)
            { (seriesMetadata, bookMetadata), (newSeriesMetadata, newBookMetadata) ->
                mergeMetadata(seriesMetadata, newSeriesMetadata.metadata, bookMetadata, newBookMetadata)
            }
    }

    private fun getAggregationMetadata(
        seriesId: KomgaSeriesId,
        searchTitles: Collection<String>,
        provider: MetadataProvider,
        edition: String?
    ): Pair<ProviderSeriesMetadata, Map<KomgaBook, BookMetadata?>>? {
        val seriesMetadata = searchTitles.firstNotNullOfOrNull {
            if (StringUtils.isAsciiPrintable(it)) {
                logger.info { "searching \"$it\" using ${provider.providerName()}" }
                provider.matchSeriesMetadata(it)
            } else null
        }
        return if (seriesMetadata == null) null
        else {
            logger.info { "found match: \"${seriesMetadata.metadata.title}\" from ${seriesMetadata.provider}  ${seriesMetadata.id}" }
            seriesMetadata to getBookMetadata(seriesId, seriesMetadata, provider, edition)
        }
    }

    private fun mergeMetadata(
        originalSeriesMetadata: SeriesMetadata,
        newSeriesMetadata: SeriesMetadata,
        originalBookMetadata: Map<KomgaBook, BookMetadata?>,
        newBookMetadata: Map<KomgaBook, BookMetadata?>,
    ): Pair<SeriesMetadata, Map<KomgaBook, BookMetadata?>> {
        val books = originalBookMetadata.keys.associateBy { it.id }
        val mergedSeries = MetadataMerger.mergeSeriesMetadata(originalSeriesMetadata, newSeriesMetadata)
        val mergedBookMetadata = MetadataMerger.mergeBookMetadata(
            originalBookMetadata.map { it.key.id to it.value }.toMap(),
            newBookMetadata.map { it.key.id to it.value }.toMap()
        ).map { (bookId, metadata) -> books[bookId]!! to metadata }.toMap()

        return mergedSeries to mergedBookMetadata
    }

}
