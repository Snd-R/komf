package org.snd.mediaserver

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.snd.config.MetadataUpdateConfig
import org.snd.mediaserver.UpdateMode.API
import org.snd.mediaserver.model.*
import org.snd.mediaserver.model.MediaServer.KAVITA
import org.snd.mediaserver.repository.MatchedBook
import org.snd.mediaserver.repository.MatchedBookRepository
import org.snd.mediaserver.repository.MatchedSeries
import org.snd.mediaserver.repository.MatchedSeriesRepository
import org.snd.metadata.BookFilenameParser
import org.snd.metadata.MetadataMerger
import org.snd.metadata.MetadataProvider
import org.snd.metadata.comicinfo.ComicInfoWriter
import org.snd.metadata.model.*
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

private val logger = KotlinLogging.logger {}

class MetadataService(
    private val mediaServerClient: MediaServerClient,
    private val metadataProviders: Map<Provider, MetadataProvider>,
    private val matchedSeriesRepository: MatchedSeriesRepository,
    private val matchedBookRepository: MatchedBookRepository,
    private val metadataUpdateConfig: MetadataUpdateConfig,
    private val metadataUpdateMapper: MetadataUpdateMapper,
    private val aggregateMetadata: Boolean,
    private val executor: ExecutorService,
    private val comicInfoWriter: ComicInfoWriter,
    private val serverType: MediaServer,
) {
    fun availableProviders(): Set<Provider> = metadataProviders.keys

    fun searchSeriesMetadata(seriesName: String): Collection<SeriesSearchResult> {
        return metadataProviders.values.flatMap { it.searchSeries(seriesName) }
    }

    fun setSeriesMetadata(
        seriesId: MediaServerSeriesId,
        providerName: Provider,
        providerSeriesId: ProviderSeriesId,
        edition: String?
    ) {
        val provider = metadataProviders[providerName] ?: throw RuntimeException()
        val series = mediaServerClient.getSeries(seriesId)

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

        if (metadataUpdateConfig.mode == UpdateMode.FILE_EMBED) {
            mediaServerClient.refreshMetadata(series.id)
        }
    }

    fun matchLibraryMetadata(libraryId: MediaServerLibraryId) {
        var errorCount = 0
        mediaServerClient.getSeries(libraryId).forEach {
            runCatching {
                matchSeriesMetadata(it.id)
            }.onFailure {
                logger.error(it) { }
                errorCount += 1
            }
        }

        logger.info { "Finished library scan. Encountered $errorCount errors" }
    }

    fun matchSeriesMetadata(seriesId: MediaServerSeriesId) {
        val series = mediaServerClient.getSeries(seriesId)
        logger.info { "attempting to match series \"${series.name}\" ${series.id}" }
        val matchResult = metadataProviders.values.asSequence()
            .mapNotNull { provider -> provider.matchSeriesMetadata(series.name)?.let { provider to it } }
            .map { (provider, seriesMetadata) ->
                logger.info { "found match: \"${seriesMetadata.metadata.title}\" from ${provider.providerName()}  ${seriesMetadata.id}" }
                val bookMetadata = getBookMetadata(series.id, seriesMetadata, provider, null)
                provider to SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata)
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

        if (metadataUpdateConfig.mode == UpdateMode.FILE_EMBED) {
            mediaServerClient.refreshMetadata(series.id)
        }
        logger.info { "finished metadata update of series \"${series.name}\" ${series.id}" }
    }

    fun resetSeriesMetadata(seriesId: MediaServerSeriesId) {
        val series = mediaServerClient.getSeries(seriesId)
        resetSeriesMetadata(series)
    }

    private fun resetSeriesMetadata(series: MediaServerSeries) {
        mediaServerClient.resetSeriesMetadata(series.id, series.name)

        mediaServerClient.getBooks(series.id)
            .forEach { resetBookMetadata(it) }

        replaceSeriesThumbnail(series.id, null)
        matchedSeriesRepository.delete(series.id, serverType)
    }

    private fun resetBookMetadata(book: MediaServerBook) {
        mediaServerClient.resetBookMetadata(book.id, book.name)

        replaceBookThumbnail(book.id, null)
        matchedBookRepository.delete(book.id, serverType)
    }

    fun resetLibraryMetadata(libraryId: MediaServerLibraryId) {
        mediaServerClient.getSeries(libraryId).forEach { resetSeriesMetadata(it) }
    }

    private fun updateSeriesMetadata(series: MediaServerSeries, metadata: SeriesMetadata) {
        if (metadataUpdateConfig.mode == API) {
            val metadataUpdate = metadataUpdateMapper.toSeriesMetadataUpdate(metadata, series.metadata)
            mediaServerClient.updateSeriesMetadata(series.id, metadataUpdate)
        }

        val newThumbnail = if (metadataUpdateConfig.seriesThumbnails) metadata.thumbnail else null
        val thumbnailId = replaceSeriesThumbnail(series.id, newThumbnail)

        if (thumbnailId == null) {
            matchedSeriesRepository.delete(series.id, serverType)
        } else {
            matchedSeriesRepository.save(
                MatchedSeries(
                    seriesId = series.id,
                    type = serverType,
                    thumbnailId = thumbnailId,
                )
            )
        }
    }

    private fun getBookMetadata(
        seriesId: MediaServerSeriesId,
        seriesMeta: ProviderSeriesMetadata,
        provider: MetadataProvider,
        bookEdition: String?
    ): Map<MediaServerBook, BookMetadata?> {
        if (serverType == KAVITA && metadataUpdateConfig.mode == API) return emptyMap()

        val books = mediaServerClient.getBooks(seriesId)
        val metadataMatch = associateBookMetadata(books, seriesMeta.books, bookEdition)

        return metadataMatch.map { (book, seriesBookMeta) ->
            if (seriesBookMeta != null) {
                logger.info { "(${provider.providerName()}) fetching metadata for book ${seriesBookMeta.name}" }
                book to provider.getBookMetadata(seriesMeta.id, seriesBookMeta.id).metadata
            } else {
                book to null
            }
        }.toMap()
    }

    private fun updateBookMetadata(bookMetadata: Map<MediaServerBook, BookMetadata?>, seriesMetadata: SeriesMetadata?) {
        bookMetadata.forEach { (book, metadata) -> updateBookMetadata(book, metadata, seriesMetadata) }
    }

    private fun updateBookMetadata(book: MediaServerBook, metadata: BookMetadata?, seriesMeta: SeriesMetadata?) {
        if (metadataUpdateConfig.mode == API) {
            metadataUpdateMapper.toBookMetadataUpdate(metadata, seriesMeta, book.metadata)
                ?.let { mediaServerClient.updateBookMetadata(book.id, it) }
        } else if (book.deleted.not()) {
            metadataUpdateMapper.toComicInfo(metadata, seriesMeta)?.let {
                comicInfoWriter.writeMetadata(Path.of(book.url), it)
            }
        }

        val newThumbnail = if (metadataUpdateConfig.bookThumbnails) metadata?.thumbnail else null
        val thumbnailId = replaceBookThumbnail(book.id, newThumbnail)

        if (thumbnailId == null) {
            matchedBookRepository.delete(book.id, serverType)
        } else {
            matchedBookRepository.save(
                MatchedBook(
                    seriesId = book.seriesId,
                    bookId = book.id,
                    type = serverType,
                    thumbnailId = thumbnailId,
                )
            )
        }
    }

    private fun replaceSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnail: Image?): MediaServerThumbnailId? {
        val matchedSeries = matchedSeriesRepository.findFor(seriesId, serverType)
        val thumbnails = mediaServerClient.getSeriesThumbnails(seriesId)

        val uploadedThumbnail = thumbnail?.let {
            mediaServerClient.uploadSeriesThumbnail(
                seriesId = seriesId,
                thumbnail = thumbnail,
                selected = thumbnails.isEmpty()
            )
        }

        matchedSeries?.thumbnailId?.let { thumb ->
            if (thumbnails.any { it.id == thumb }) {
                mediaServerClient.deleteSeriesThumbnail(seriesId, thumb)
            }
        }

        return uploadedThumbnail?.id
    }

    private fun replaceBookThumbnail(bookId: MediaServerBookId, thumbnail: Image?): MediaServerThumbnailId? {
        val existingMatch = matchedBookRepository.findFor(bookId, serverType)
        val thumbnails = mediaServerClient.getBookThumbnails(bookId)

        val uploadedThumbnail = thumbnail?.let {
            mediaServerClient.uploadBookThumbnail(
                bookId = bookId,
                thumbnail = thumbnail,
                selected = thumbnails.all { it.type == "GENERATED" || it.id == existingMatch?.thumbnailId }
            )
        }

        existingMatch?.thumbnailId?.let { thumb ->
            if (thumbnails.any { it.id == thumb }) {
                mediaServerClient.deleteBookThumbnail(bookId, thumb)
            }
        }

        return uploadedThumbnail?.id
    }

    private fun associateBookMetadata(
        books: Collection<MediaServerBook>,
        providerBooks: Collection<SeriesBook>,
        edition: String? = null
    ): Map<MediaServerBook, SeriesBook?> {
        val editions = providerBooks.groupBy { it.edition }
        val noEditionBooks = providerBooks.filter { it.edition == null }

        if (edition != null) {
            val editionName = edition.replace("(?i)\\s?[EÉ]dition\\s?".toRegex(), "").lowercase()
            return books.associateWith { komgaBook ->
                val volume = BookFilenameParser.getVolumes(komgaBook.name)?.first ?: komgaBook.number
                editions[editionName]?.firstOrNull { volume == it.number }
            }
        }

        val byEdition: Map<MediaServerBook, String?> = books.associateWith { book ->
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
        series: MediaServerSeries,
        metadata: SeriesAndBookMetadata,
        providers: Collection<MetadataProvider>,
        edition: String?
    ): SeriesAndBookMetadata {
        if (providers.isEmpty()) return metadata
        logger.info { "launching metadata aggregation using ${providers.map { it.providerName() }}" }

        val searchTitles =
            setOfNotNull(series.name, metadata.seriesMetadata?.title) + (metadata.seriesMetadata?.alternativeTitles
                ?: emptySet())

        return providers.map { provider ->
            CompletableFuture.supplyAsync({
                getAggregationMetadata(
                    series,
                    searchTitles,
                    provider,
                    edition
                )
            }, executor)
        }
            .mapNotNull { it.join() }
            .fold(metadata) { oldMetadata, newMetadata -> mergeMetadata(oldMetadata, newMetadata) }
    }

    private fun getAggregationMetadata(
        series: MediaServerSeries,
        searchTitles: Collection<String>,
        provider: MetadataProvider,
        bookEdition: String?
    ): SeriesAndBookMetadata? {
        return searchTitles.asSequence()
            .filter { StringUtils.isAsciiPrintable(it) }
            .onEach { logger.info { "searching \"$it\" using ${provider.providerName()}" } }
            .mapNotNull { provider.matchSeriesMetadata(it) }
            .map { seriesMetadata ->
                logger.info { "found match: \"${seriesMetadata.metadata.title}\" from ${provider.providerName()}  ${seriesMetadata.id}" }
                val bookMetadata = getBookMetadata(series.id, seriesMetadata, provider, bookEdition)
                SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata)
            }
            .firstOrNull()
    }

    private fun mergeMetadata(
        originalMetadata: SeriesAndBookMetadata,
        newMetadata: SeriesAndBookMetadata
    ): SeriesAndBookMetadata {
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
}
