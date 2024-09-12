package snd.komf.mediaserver.metadata

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.jobs.KomfJobTracker
import snd.komf.mediaserver.jobs.MetadataJobEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.CompletionEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.PostProcessingStartEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProcessingErrorEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderBookEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderCompletedEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderErrorEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderSeriesEvent
import snd.komf.mediaserver.jobs.MetadataJobId
import snd.komf.mediaserver.metadata.repository.SeriesMatchRepository
import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeries
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.SeriesAndBookMetadata
import snd.komf.model.BookMetadata
import snd.komf.model.BookQualifier
import snd.komf.model.BookRange
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.MatchType.MANUAL
import snd.komf.model.MediaType
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesBook
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataProvider
import snd.komf.providers.ProviderFactory
import snd.komf.util.BookNameParser

private val logger = KotlinLogging.logger {}

class MetadataService(
    private val mediaServerClient: MediaServerClient,
    private val metadataProviders: ProviderFactory.MetadataProviders,
    private val aggregateMetadata: Boolean,
    private val metadataMerger: MetadataMerger,
    private val metadataUpdateService: MetadataUpdater,
    private val seriesMatchRepository: SeriesMatchRepository,
    private val libraryType: MediaType,
    private val jobTracker: KomfJobTracker,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun availableProviders(libraryId: MediaServerLibraryId) = metadataProviders.providers(libraryId.value)
    fun availableProviders() = metadataProviders.defaultProvidersList()

    suspend fun searchSeriesMetadata(
        seriesName: String,
        libraryId: MediaServerLibraryId
    ): Collection<SeriesSearchResult> {
        val providers = metadataProviders.providers(libraryId.value)

        return providers
            .map { coroutineScope.async { it.searchSeries(seriesName) } }
            .flatMap { it.await() }
    }

    suspend fun searchSeriesMetadata(seriesName: String): Collection<SeriesSearchResult> {
        val providers = metadataProviders.defaultProvidersList()
        return providers
            .map { coroutineScope.async { it.searchSeries(seriesName) } }
            .flatMap { it.await() }
    }

    suspend fun getSeriesCover(
        libraryId: MediaServerLibraryId,
        providerName: CoreProviders,
        providerSeriesId: ProviderSeriesId,
    ): Image? {
        val provider = checkNotNull(metadataProviders.provider(libraryId.value, providerName)) {
            "Provider $providerName is not enabled for library $libraryId"
        }
        return provider.getSeriesCover(providerSeriesId)
    }

    fun setSeriesMetadata(
        seriesId: MediaServerSeriesId,
        providerName: CoreProviders,
        providerSeriesId: ProviderSeriesId,
        edition: String?
    ): MetadataJobId {
        val jobId = launchJob(seriesId) { eventFlow ->
            val series = mediaServerClient.getSeries(seriesId)
            val books = mediaServerClient.getBooks(seriesId)
            val seriesTitle = series.metadata.title.ifBlank { series.name }
            logger.info { "Setting metadata for series \"${seriesTitle}\" ${series.id} using $providerName $providerSeriesId" }
            val provider =
                metadataProviders.provider(series.libraryId.value, providerName) ?: throw RuntimeException()

            val seriesMetadata = getSeriesMetadata(provider, providerSeriesId, eventFlow)
            val bookMetadata = getBookMetadata(books, seriesMetadata, provider, edition, eventFlow)
            eventFlow.emit(ProviderCompletedEvent(providerName))

            val metadata = if (aggregateMetadata) {
                aggregateMetadataFromProviders(
                    series = series,
                    books = books,
                    metadata = SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata),
                    providers = metadataProviders.providers(series.libraryId.value).filter { it != provider },
                    edition = edition,
                    eventFlow = eventFlow
                )
            } else SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata)

            eventFlow.emit(PostProcessingStartEvent)
            metadataUpdateService.updateMetadata(series, metadata)
            seriesMatchRepository.save(
                seriesId = series.id,
                type = MANUAL,
                provider = providerName,
                providerSeriesId = providerSeriesId,
            )
            logger.info { "finished metadata update of series \"${seriesTitle}\" ${series.id}" }
        }
        return jobId
    }

    fun matchLibraryMetadata(libraryId: MediaServerLibraryId) {
        coroutineScope.launch {
            var errorCount = 0
            var pageNumber = 1
            do {
                val page = mediaServerClient.getSeries(libraryId, pageNumber)
                page.content.forEach {
                    runCatching {
                        jobTracker.getMetadataJobEvents(matchSeriesMetadata(it.id))
                            ?.takeWhile { it !is CompletionEvent }
                            ?.collect()
                    }
                        .onFailure {
                            logger.error(it) { }
                            errorCount += 1
                        }
                }
                pageNumber++
            } while (page.pageNumber != page.totalPages || page.content.isNotEmpty())
            logger.info { "Finished library scan. Encountered $errorCount errors" }
        }
    }

    fun matchSeriesMetadata(
        seriesId: MediaServerSeriesId,
    ): MetadataJobId {

        val jobId = launchJob(seriesId) { eventFlow ->
            val series = mediaServerClient.getSeries(seriesId)
            val books = mediaServerClient.getBooks(seriesId)
            val seriesTitle = series.metadata.title.ifBlank { series.name }

            val existingMatch = seriesMatchRepository.findManualFor(seriesId)
            val matchProvider =
                existingMatch?.provider?.let { metadataProviders.provider(series.libraryId.value, it) }

            val matchResult = if (existingMatch != null && matchProvider != null) {
                logger.info { "using ${matchProvider.providerName()} from previous manual identification for $seriesTitle ${series.id}" }
                val seriesMetadata = getSeriesMetadata(matchProvider, existingMatch.providerSeriesId, eventFlow)
                val bookMetadata = getBookMetadata(books, seriesMetadata, matchProvider, null, eventFlow)
                matchProvider to SeriesAndBookMetadata(seriesMetadata.metadata, bookMetadata)
            } else {
                val searchTitles = listOfNotNull(
                    seriesTitle,
                    removeParentheses(seriesTitle).let { if (it == seriesTitle) null else it }
                ).plus(series.metadata.alternativeTitles.map { it.title })

                logger.info { "attempting to match series \"${seriesTitle}\" ${series.id}" }

                metadataProviders.providers(series.libraryId.value).firstNotNullOfOrNull { provider ->
                    matchSeries(series, books, searchTitles, provider, null, eventFlow)
                        ?.let { provider to it }
                }
            }

            if (matchResult == null) {
                logger.info { "no match found for series $seriesTitle ${series.id}" }
                return@launchJob
            }
            eventFlow.emit(ProviderCompletedEvent(matchResult.first.providerName()))

            val metadata = matchResult.let { (provider, metadata) ->
                if (aggregateMetadata) {
                    aggregateMetadataFromProviders(
                        series = series,
                        books = books,
                        metadata = metadata,
                        providers = metadataProviders.providers(series.libraryId.value).filter { it != provider },
                        edition = null,
                        eventFlow = eventFlow
                    )
                } else metadata
            }

            eventFlow.emit(PostProcessingStartEvent)
            metadataUpdateService.updateMetadata(series, metadata)
            logger.info { "finished metadata update of series \"${seriesTitle}\" ${series.id}" }
        }

        return jobId
    }

    private suspend fun matchSeries(
        series: MediaServerSeries,
        books: Collection<MediaServerBook>,
        searchTitles: Collection<String>,
        provider: MetadataProvider,
        bookEdition: String?,
        eventFlow: MutableSharedFlow<MetadataJobEvent>
    ): SeriesAndBookMetadata? {
        for (searchTitle in searchTitles) {
            logger.info { "searching \"$searchTitle\" using ${provider.providerName()}" }

            eventFlow.emit(ProviderSeriesEvent(provider.providerName()))
            val result = try {
                provider.matchSeriesMetadata(createMatchQuery(searchTitle, series, books))
            } catch (e: Exception) {
                throw ProviderException(provider.providerName(), e)
            }

            if (result != null) {
                logger.info { "found match: \"${result.metadata.titles.firstOrNull()?.name}\" from ${provider.providerName()}  ${result.id}" }
                val bookMetadata = getBookMetadata(books, result, provider, bookEdition, eventFlow)
                return SeriesAndBookMetadata(result.metadata, bookMetadata)
            }
        }
        return null
    }

    private suspend fun getBookMetadata(
        books: Collection<MediaServerBook>,
        seriesMeta: ProviderSeriesMetadata,
        provider: MetadataProvider,
        bookEdition: String?,
        eventFlow: MutableSharedFlow<MetadataJobEvent>,
    ): Map<MediaServerBook, BookMetadata?> {
        val metadataMatch = associateBookMetadata(books, seriesMeta.books, bookEdition)

        val fetchSize = metadataMatch.filterValues { it != null }.size
        var progress = 1
        return try {
            metadataMatch.map { (book, seriesBookMeta) ->
                if (seriesBookMeta != null) {
                    logger.info { "(${provider.providerName()}) fetching metadata for book ${seriesBookMeta.name}" }
                    eventFlow.emit(ProviderBookEvent(provider.providerName(), fetchSize, progress))
                    progress += 1

                    book to provider.getBookMetadata(seriesMeta.id, seriesBookMeta.id).metadata
                } else {
                    book to null
                }
            }.toMap()

        } catch (e: Exception) {
            throw ProviderException(provider.providerName(), e)
        }
    }

    private fun associateBookMetadata(
        books: Collection<MediaServerBook>,
        providerBooks: Collection<SeriesBook>,
        edition: String? = null
    ): Map<MediaServerBook, SeriesBook?> {
        val editionBooks = providerBooks.groupBy { it.edition }

        if (edition != null) {
            val editionName = edition.replace("(?i)\\s?[EÉ]dition\\s?".toRegex(), "").lowercase()
            return books.associateWith { book ->
                val bookNumber = getBookNumber(book.name)
                editionBooks[editionName]?.firstOrNull { it.number != null && bookNumber == it.number }
            }
        }

        if (books.size == 1 && providerBooks.size == 1) {
            val mediaServerBook = books.first()
            val chapterNumber = BookNameParser.getChapters(mediaServerBook.name)

            return if (chapterNumber == null)
                mapOf(books.first() to providerBooks.first())
            else mapOf(books.first() to null)
        }

        return books.associateWith { book ->
            val bookExtraData = BookNameParser.getExtraData(book.name).map { it.lowercase() }
            editionBooks.keys.firstOrNull { bookExtraData.contains(it) }
        }.map { (book, edition) ->
            val bookNumber = getBookNumber(book.name)
            val providerBook = editionBooks[edition]
                ?.firstOrNull { it.number != null && it.number == bookNumber }
            book to providerBook
        }.toMap()
    }

    private fun getBookNumber(bookName: String): BookRange? {
        return when (libraryType) {
            MediaType.MANGA -> BookNameParser.getVolumes(bookName)
            MediaType.NOVEL, MediaType.COMIC -> BookNameParser.getBookNumber(bookName)
        }
    }

    private suspend fun aggregateMetadataFromProviders(
        series: MediaServerSeries,
        books: Collection<MediaServerBook>,
        metadata: SeriesAndBookMetadata,
        providers: Collection<MetadataProvider>,
        edition: String?,
        eventFlow: MutableSharedFlow<MetadataJobEvent>
    ): SeriesAndBookMetadata {
        if (providers.isEmpty()) return metadata
        logger.info { "launching metadata aggregation using ${providers.map { it.providerName() }}" }

        val searchTitles = metadata.seriesMetadata.titles
            .map { it.name }

        return providers
            .map { provider ->
                coroutineScope.async {
                    matchSeries(
                        series = series,
                        books = books,
                        searchTitles = searchTitles,
                        provider = provider,
                        bookEdition = edition,
                        eventFlow = eventFlow
                    ).also {
                        eventFlow.emit(ProviderCompletedEvent(provider.providerName()))
                    }

                }
            }
            .mapNotNull { it.await() }
            .fold(metadata) { oldMetadata, newMetadata -> mergeMetadata(oldMetadata, newMetadata) }
    }

    private fun mergeMetadata(
        originalMetadata: SeriesAndBookMetadata,
        newMetadata: SeriesAndBookMetadata
    ): SeriesAndBookMetadata {
        val mergedSeries =
            metadataMerger.mergeSeriesMetadata(originalMetadata.seriesMetadata, newMetadata.seriesMetadata)

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

    private suspend fun createMatchQuery(
        searchTitle: String,
        series: MediaServerSeries,
        books: Collection<MediaServerBook>
    ): MatchQuery {
        val (firstBook, range) = books.sortedBy { it.number }.map { book ->
            book to (BookNameParser.getVolumes(book.name) ?: BookRange(book.number))
        }.minBy { (_, number) -> number.start }
        val cover = mediaServerClient.getBookThumbnail(firstBook.id)
        val releaseYear = series.metadata.releaseYear?.let { if (it == 0) null else it }

        return MatchQuery(searchTitle, releaseYear, BookQualifier(firstBook.name, range, cover))
    }

    private suspend fun getSeriesMetadata(
        provider: MetadataProvider,
        providerSeriesId: ProviderSeriesId,
        eventFlow: MutableSharedFlow<MetadataJobEvent>
    ): ProviderSeriesMetadata {
        eventFlow.emit(ProviderSeriesEvent(provider.providerName()))

        return try {
            provider.getSeriesMetadata(providerSeriesId)
        } catch (e: Exception) {
            throw ProviderException(provider.providerName(), e)
        }

    }

    private fun launchJob(
        seriesId: MediaServerSeriesId,
        block: suspend (eventFlow: MutableSharedFlow<MetadataJobEvent>) -> Unit
    ): MetadataJobId {
        val eventFlow = MutableSharedFlow<MetadataJobEvent>(
            replay = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val jobId = jobTracker.registerMetadataJob(seriesId, eventFlow)

        coroutineScope.launch {
            try {
                block(eventFlow)
            } catch (providerException: ProviderException) {
                val errorMessage = providerException.cause?.let { cause ->
                    if (cause is ResponseException) {
                        "${cause::class.simpleName}: status code ${cause.response.status} ${cause.response.bodyAsText()}"
                    } else {
                        "${cause::class.simpleName}: ${cause.message}"
                    }
                } ?: "Unknown error"

                eventFlow.emit(
                    ProviderErrorEvent(
                        provider = providerException.provider,
                        message = errorMessage
                    )
                )
                logger.catching(providerException)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ResponseException) {
                eventFlow.emit(
                    ProcessingErrorEvent(
                        "${exception::class.simpleName}: status code ${exception.response.status} ${exception.response.bodyAsText()}"
                    )
                )
                logger.catching(exception)
            } catch (exception: Exception) {
                eventFlow.emit(ProcessingErrorEvent("${exception::class.simpleName}: ${exception.message}"))
                logger.catching(exception)
            } finally {
                eventFlow.emit(CompletionEvent)
            }
        }

        return jobId
    }


    private class ProviderException(
        val provider: CoreProviders,
        cause: Throwable,
    ) : RuntimeException(cause)

}
