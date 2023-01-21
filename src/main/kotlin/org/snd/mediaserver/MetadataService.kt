package org.snd.mediaserver

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.snd.mediaserver.model.MatchType.MANUAL
import org.snd.mediaserver.model.MediaServerBook
import org.snd.mediaserver.model.MediaServerLibraryId
import org.snd.mediaserver.model.MediaServerSeries
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.mediaserver.model.SeriesAndBookMetadata
import org.snd.mediaserver.repository.SeriesMatch
import org.snd.mediaserver.repository.SeriesMatchRepository
import org.snd.metadata.BookNameParser
import org.snd.metadata.MetadataMerger
import org.snd.metadata.MetadataProvider
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.Provider
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesBook
import org.snd.metadata.model.SeriesSearchResult
import org.snd.module.MetadataModule.MetadataProviders
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

private val logger = KotlinLogging.logger {}

class MetadataService(
    private val mediaServerClient: MediaServerClient,
    private val metadataProviders: MetadataProviders,
    private val aggregateMetadata: Boolean,
    private val executor: ExecutorService,
    private val metadataUpdateService: MetadataUpdateService,
    private val seriesMatchRepository: SeriesMatchRepository,
    private val postProcessor: MetadataPostProcessor,
) {
    fun availableProviders(libraryId: MediaServerLibraryId) = metadataProviders.providers(libraryId.id)

    fun availableProviders() = metadataProviders.defaultProviders()

    fun searchSeriesMetadata(seriesName: String, libraryId: MediaServerLibraryId): Collection<SeriesSearchResult> {
        val providers = metadataProviders.providers(libraryId.id)
        return providers.flatMap { it.searchSeries(seriesName) }
    }

    fun searchSeriesMetadata(seriesName: String): Collection<SeriesSearchResult> {
        val providers = metadataProviders.defaultProviders()
        return providers.flatMap { it.searchSeries(seriesName) }
    }

    fun setSeriesMetadata(
        seriesId: MediaServerSeriesId,
        providerName: Provider,
        providerSeriesId: ProviderSeriesId,
        edition: String?
    ) {
        val series = mediaServerClient.getSeries(seriesId)
        val seriesTitle = series.metadata.title.ifBlank { series.name }
        logger.info { "Setting metadata for series \"${seriesTitle}\" ${series.id} using $providerName $providerSeriesId" }
        val provider = metadataProviders.provider(series.libraryId.id, providerName) ?: throw RuntimeException()

        val seriesMetadata = provider.getSeriesMetadata(providerSeriesId)
        val bookMetadata = getBookMetadata(seriesId, seriesMetadata, provider, edition)

        val metadata = if (aggregateMetadata) {
            aggregateMetadataFromProviders(
                series,
                SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata),
                metadataProviders.providers(series.libraryId.id).filter { it != provider },
                edition
            )
        } else SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata)

        metadataUpdateService.updateMetadata(series, postProcessor.process(metadata))
        seriesMatchRepository.save(
            SeriesMatch(
                seriesId = series.id,
                type = MANUAL,
                provider = providerName,
                providerSeriesId = providerSeriesId,
                edition = edition
            )
        )
        logger.info { "finished metadata update of series \"${seriesTitle}\" ${series.id}" }
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
        val seriesTitle = series.metadata.title.ifBlank { series.name }

        val existingMatch = seriesMatchRepository.findManualFor(seriesId)
        val matchProvider = existingMatch?.provider?.let { metadataProviders.provider(series.libraryId.id, it) }

        val matchResult = if (existingMatch != null && matchProvider != null) {
            logger.info { "using ${matchProvider.providerName()} from previous manual identification for $seriesTitle ${series.id}" }
            val seriesMetadata = matchProvider.getSeriesMetadata(existingMatch.providerSeriesId)
            val bookMetadata = getBookMetadata(seriesId, seriesMetadata, matchProvider, existingMatch.edition)
            matchProvider to SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata)
        } else {
            logger.info { "attempting to match series \"${seriesTitle}\" ${series.id}" }
            metadataProviders.providers(series.libraryId.id).asSequence()
                .mapNotNull { provider -> provider.matchSeriesMetadata(seriesTitle)?.let { provider to it } }
                .map { (provider, seriesMetadata) ->
                    logger.info { "found match: \"${seriesMetadata.metadata.titles.firstOrNull()?.name}\" from ${provider.providerName()}  ${seriesMetadata.id}" }
                    val bookMetadata = getBookMetadata(series.id, seriesMetadata, provider, null)
                    provider to SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata)
                }
                .firstOrNull()
        }

        if (matchResult == null) {
            logger.info { "no match found for series $seriesTitle ${series.id}" }
            return
        }

        val metadata = matchResult.let { (provider, metadata) ->
            if (aggregateMetadata) {
                aggregateMetadataFromProviders(
                    series,
                    metadata,
                    metadataProviders.providers(series.libraryId.id).filter { it != provider },
                    null
                )
            } else metadata
        }

        metadataUpdateService.updateMetadata(series, postProcessor.process(metadata))
        logger.info { "finished metadata update of series \"${seriesTitle}\" ${series.id}" }
    }

    private fun getBookMetadata(
        seriesId: MediaServerSeriesId,
        seriesMeta: ProviderSeriesMetadata,
        provider: MetadataProvider,
        bookEdition: String?
    ): Map<MediaServerBook, BookMetadata?> {
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

    private fun associateBookMetadata(
        books: Collection<MediaServerBook>,
        providerBooks: Collection<SeriesBook>,
        edition: String? = null
    ): Map<MediaServerBook, SeriesBook?> {
        val editions = providerBooks.groupBy { it.edition }
        val noEditionBooks = providerBooks.filter { it.edition == null }

        if (edition != null) {
            val editionName = edition.replace("(?i)\\s?[EÉ]dition\\s?".toRegex(), "").lowercase()
            return books.associateWith { book ->
                val volume = BookNameParser.getVolumes(book.name) ?: book.number..book.number
                editions[editionName]?.firstOrNull { it.number != null && volume == it.number }
            }
        }

        val byEdition: Map<MediaServerBook, String?> = books.associateWith { book ->
            val bookExtraData = BookNameParser.getExtraData(book.name).map { it.lowercase() }
            editions.keys.firstOrNull { bookExtraData.contains(it) }
        }

        return byEdition.map { (book, edition) ->
            val volumes = BookNameParser.getVolumes(book.name)
            val matched = if (edition == null) {
                noEditionBooks.firstOrNull { it.number != null && it.number == volumes }
            } else {
                editions[edition]?.firstOrNull { it.number != null && it.number == volumes }
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

        val searchTitles = listOf(series.metadata.title.ifBlank { series.name }) +
                (metadata.seriesMetadata?.titles?.map { it.name } ?: emptySet())

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
                logger.info { "found match: \"${seriesMetadata.metadata.titles.firstOrNull()?.name}\" from ${provider.providerName()}  ${seriesMetadata.id}" }
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
