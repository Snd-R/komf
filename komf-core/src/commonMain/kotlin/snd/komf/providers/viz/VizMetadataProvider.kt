package snd.komf.providers.viz

import io.ktor.client.plugins.*
import io.ktor.http.*
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.CoreProviders.VIZ
import snd.komf.providers.MetadataProvider
import snd.komf.providers.viz.model.VizBook
import snd.komf.providers.viz.model.VizBookId
import snd.komf.providers.viz.model.VizBookReleaseType.DIGITAL
import snd.komf.providers.viz.model.VizBookReleaseType.PAPERBACK
import snd.komf.providers.viz.model.toVizSeriesBook
import snd.komf.util.NameSimilarityMatcher

class VizMetadataProvider(
    private val client: VizClient,
    private val metadataMapper: VizMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {
    override fun providerName(): CoreProviders {
        return VIZ
    }

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = getBook(VizBookId(seriesId.value))
        val books = series.allBooksId
            ?.let { client.getAllBooks(it) }
            ?: listOf(series.toVizSeriesBook())
        val thumbnail = if (fetchSeriesCovers) getThumbnail(series.coverUrl) else null

        return metadataMapper.toSeriesMetadata(series, books, thumbnail)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = getBook(VizBookId(bookId.id))
        val thumbnail = if (fetchBookCovers) getThumbnail(bookMetadata.coverUrl) else null

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        if (isInvalidName(seriesName)) return emptyList()

        val searchResults = client.searchSeries(sanitizeSearchInput(seriesName.take(100))).take(limit)
        return searchResults.map { metadataMapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        if (isInvalidName(seriesName)) return null
        val searchResults = client.searchSeries(sanitizeSearchInput(seriesName.take(100)))

        return searchResults
            .firstOrNull { nameMatcher.matches(seriesName, it.seriesName) }
            ?.let {
                val firstBook = getBook(it.id)
                val books = firstBook.allBooksId
                    ?.let { id -> client.getAllBooks(id) }
                    ?: listOf(firstBook.toVizSeriesBook())
                val thumbnail = if (fetchSeriesCovers) getThumbnail(firstBook.coverUrl) else null
                metadataMapper.toSeriesMetadata(firstBook, books, thumbnail)
            }
    }

    private suspend fun getBook(id: VizBookId): VizBook {
        return try {
            client.getBook(id, DIGITAL)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) {
                client.getBook(id, PAPERBACK)
            } else throw e
        }
    }

    private suspend fun getThumbnail(url: String?): Image? = url?.let { client.getThumbnail(it) }

    private fun isInvalidName(name: String) = name.contains("^[0-9]+--".toRegex())

    private fun sanitizeSearchInput(name: String): String {
        return name
            .replace("[(]([^)]+)[)]".toRegex(), "")
            .trim()
    }
}
