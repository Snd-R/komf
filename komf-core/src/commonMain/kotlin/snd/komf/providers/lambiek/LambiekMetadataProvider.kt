package snd.komf.providers.lambiek

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.CoreProviders.LAMBIEK
import snd.komf.providers.MetadataProvider
import snd.komf.providers.lambiek.model.LambiekBookId
import snd.komf.providers.lambiek.model.LambiekSeries
import snd.komf.providers.lambiek.model.LambiekSeriesId
import snd.komf.util.NameSimilarityMatcher

class LambiekMetadataProvider(
    private val client: LambiekClient,
    private val metadataMapper: LambiekMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {

    override fun providerName(): CoreProviders = LAMBIEK

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = fetchFullSeries(LambiekSeriesId(seriesId.value))
        val thumbnail = if (fetchSeriesCovers) fetchSeriesCover(series) else null
        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = fetchFullSeries(LambiekSeriesId(seriesId.value))
        return fetchSeriesCover(series)
    }

    private suspend fun fetchFullSeries(id: LambiekSeriesId): LambiekSeries {
        val seriesPage = client.getSeriesPage(id)
        val books = client.getCollections(id)
        return seriesPage.copy(books = books)
    }

    private suspend fun fetchSeriesCover(series: LambiekSeries): Image? {
        val firstBook = series.books.firstOrNull() ?: return null
        return firstBook.imageUrl?.let { client.getCover(it) }
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val lambiekSeriesId = LambiekSeriesId(seriesId.value)
        val lambiekBookId = LambiekBookId(bookId.id.toInt())

        // Find book slug from collections to construct the full URL
        val books = client.getCollections(lambiekSeriesId)
        val seriesBook = books.find { it.id == lambiekBookId }

        val book = if (seriesBook != null) {
            client.getBook(lambiekSeriesId, seriesBook)
        } else {
            // Fallback: fetch series page which shows the latest book at /shop/series/{slug}/{id}/*
            // Try to parse bookId from the series page redirect
            val fallbackBook = client.getBookById(lambiekSeriesId, lambiekBookId, bookId.id)
            fallbackBook
        }

        val thumbnail = if (fetchBookCovers) book.imageUrl?.let { client.getCover(it) } else null
        return metadataMapper.toBookMetadata(book, thumbnail)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val results = client.searchSeries(seriesName).take(limit)
        return coroutineScope {
            results.map { result ->
                async {
                    val imageUrl = try {
                        client.getCollections(result.id).firstOrNull()?.imageUrl
                    } catch (_: Exception) { null }
                    metadataMapper.toSeriesSearchResult(result, imageUrl)
                }
            }.awaitAll()
        }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(matchQuery.seriesName)
        val match = searchResults
            .firstOrNull { nameMatcher.matches(matchQuery.seriesName, listOf(it.seriesName)) }

        return match?.let {
            val series = fetchFullSeries(it.id)
            val thumbnail = if (fetchSeriesCovers) fetchSeriesCover(series) else null
            metadataMapper.toSeriesMetadata(series, thumbnail)
        }
    }
}
