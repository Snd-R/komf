package org.snd.mediaserver

import mu.KotlinLogging
import org.snd.mediaserver.model.MatchType.MANUAL
import org.snd.mediaserver.model.SeriesAndBookMetadata
import org.snd.mediaserver.model.SeriesMatch
import org.snd.mediaserver.model.mediaserver.MediaServerBook
import org.snd.mediaserver.model.mediaserver.MediaServerLibraryId
import org.snd.mediaserver.model.mediaserver.MediaServerSeries
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId
import org.snd.mediaserver.repository.SeriesMatchRepository
import org.snd.metadata.BookNameParser
import org.snd.metadata.MetadataProvider
import org.snd.metadata.model.MatchQuery
import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.BookRange
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.SeriesBook
import org.snd.module.MetadataModule.MetadataProviders
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.ExecutorService

private val logger = KotlinLogging.logger {}

class MetadataService(
    private val mediaServerClient: MediaServerClient,
    private val metadataProviders: MetadataProviders,
    private val aggregateMetadata: Boolean,
    private val metadataMerger: MetadataMerger,
    private val executor: ExecutorService,
    private val metadataUpdateService: MetadataUpdateService,
    private val seriesMatchRepository: SeriesMatchRepository,
) {
    fun availableProviders(libraryId: MediaServerLibraryId) = metadataProviders.providers(libraryId.id)

    fun availableProviders() = metadataProviders.defaultProvidersList()

    fun searchSeriesMetadata(seriesName: String, libraryId: MediaServerLibraryId): Collection<SeriesSearchResult> {
        val providers = metadataProviders.providers(libraryId.id)

        return providers
            .map { supplyAsync({ it.searchSeries(seriesName) }, executor) }
            .map { it.join() }
            .flatten()
    }

    fun searchSeriesMetadata(seriesName: String): Collection<SeriesSearchResult> {
        val providers = metadataProviders.defaultProvidersList()
        return providers
            .map { supplyAsync({ it.searchSeries(seriesName) }, executor) }
            .map { it.join() }
            .flatten()
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

        metadataUpdateService.updateMetadata(series, metadata)
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
            matchSeries(series)
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

        metadataUpdateService.updateMetadata(series, metadata)
        logger.info { "finished metadata update of series \"${seriesTitle}\" ${series.id}" }
    }

    private fun matchSeries(series: MediaServerSeries): Pair<MetadataProvider, SeriesAndBookMetadata>? {
        val seriesTitle = series.metadata.title.ifBlank { series.name }
        val searchTitles = listOfNotNull(
            seriesTitle,
            removeParentheses(seriesTitle).let { if (it == seriesTitle) null else it }
        ).plus(series.metadata.alternativeTitles.map { it.title })

        logger.info { "attempting to match series \"${seriesTitle}\" ${series.id}" }

        return metadataProviders.providers(series.libraryId.id).asSequence()
            .mapNotNull { provider ->
                matchSeries(series, searchTitles, provider, null)
                    ?.let { provider to it }
            }
            .firstOrNull()
    }

    private fun matchSeries(
        series: MediaServerSeries,
        searchTitles: Collection<String>,
        provider: MetadataProvider,
        bookEdition: String?
    ): SeriesAndBookMetadata? {
        return searchTitles.asSequence()
            .onEach { logger.info { "searching \"$it\" using ${provider.providerName()}" } }
            .mapNotNull { provider.matchSeriesMetadata(MatchQuery(it, null)) }
            .map { seriesMetadata ->
                logger.info { "found match: \"${seriesMetadata.metadata.titles.firstOrNull()?.name}\" from ${provider.providerName()}  ${seriesMetadata.id}" }
                val bookMetadata = getBookMetadata(series.id, seriesMetadata, provider, bookEdition)
                SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata)
            }
            .firstOrNull()
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
                val volume = BookNameParser.getVolumes(book.name) ?: BookRange(book.number.toDouble(), book.number.toDouble())
                editions[editionName]?.firstOrNull { it.number != null && volume == it.number }
            }
        }

        val byEdition: Map<MediaServerBook, String?> = books.associateWith { book ->
            val bookExtraData = BookNameParser.getExtraData(book.name).map { it.lowercase() }
            editions.keys.firstOrNull { bookExtraData.contains(it) }
        }

        return byEdition.map { (book, edition) ->
            val volumes = BookNameParser.getVolumes(book.name) ?: BookRange(book.number.toDouble(), book.number.toDouble())
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

        val searchTitles = metadata.seriesMetadata.titles
            .map { it.name }

        return providers.map { provider ->
            supplyAsync({
                matchSeries(
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

    private fun mergeMetadata(
        originalMetadata: SeriesAndBookMetadata,
        newMetadata: SeriesAndBookMetadata
    ): SeriesAndBookMetadata {
        val mergedSeries = metadataMerger.mergeSeriesMetadata(originalMetadata.seriesMetadata, newMetadata.seriesMetadata)

        val books = originalMetadata.bookMetadata.keys.associateBy { it.id }
        val mergedBooks = metadataMerger.mergeBookMetadata(
            originalMetadata.bookMetadata.map { it.key.id to it.value }.toMap(),
            newMetadata.bookMetadata.map { it.key.id to it.value }.toMap()
        ).map { (bookId, metadata) -> books[bookId]!! to metadata }.toMap()

        return SeriesAndBookMetadata(mergedSeries, mergedBooks)
    }

    private fun removeParentheses(name: String): String {
        return name.replace("[(\\[{]([^)\\]}]+)[)\\]}]".toRegex(), "").trim()
    }
}
