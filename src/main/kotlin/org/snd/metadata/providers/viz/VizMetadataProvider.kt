package org.snd.metadata.providers.viz

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Image
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.VIZ
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesMatchResult
import org.snd.metadata.model.SeriesMatchStatus.MATCHED
import org.snd.metadata.model.SeriesMatchStatus.NO_MATCH
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.providers.viz.model.VizBookId
import org.snd.metadata.providers.viz.model.toSeriesSearchResult

class VizMetadataProvider(
    private val client: VizClient,
    private val metadataMapper: VizMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
) : MetadataProvider {
    override fun providerName(): Provider {
        return VIZ
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getBook(VizBookId(seriesId.id))
        val books = client.getAllBooks(series.allBooksId)
        val thumbnail = getThumbnail(series.coverUrl)

        return metadataMapper.toSeriesMetadata(series, books, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(VizBookId(bookId.id))
        val thumbnail = getThumbnail(bookMetadata.coverUrl)

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        if (isInvalidName(seriesName)) return emptyList()

        val searchResults = client.searchSeries(seriesName.take(100)).take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): SeriesMatchResult {
        if (isInvalidName(seriesName)) return SeriesMatchResult(NO_MATCH, null)
        val searchResults = client.searchSeries(seriesName.take(100))

        val metadata = searchResults
            .firstOrNull { nameMatcher.matches(seriesName, it.seriesName) }
            ?.let {
                val firstBook = client.getBook(it.id)
                val books = client.getAllBooks(firstBook.allBooksId)
                val thumbnail = getThumbnail(firstBook.coverUrl)
                metadataMapper.toSeriesMetadata(firstBook, books, thumbnail)
            }

        return SeriesMatchResult(
            status = if (metadata == null) NO_MATCH else MATCHED,
            result = metadata
        )
    }

    private fun getThumbnail(url: String?): Image? = url?.toHttpUrl()?.let { client.getThumbnail(it) }

    private fun isInvalidName(name: String) = name.contains("^[0-9]+--".toRegex())
}
