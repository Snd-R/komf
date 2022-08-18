package org.snd.metadata.yenpress

import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.snd.metadata.MetadataProvider
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.YEN_PRESS
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.yenpress.model.YenPressBookId
import org.snd.metadata.yenpress.model.YenPressSearchResult
import org.snd.metadata.yenpress.model.toSeriesSearchResult

class YenPressMetadataProvider(
    private val client: YenPressClient,
    private val metadataMapper: YenPressMetadataMapper,
) : MetadataProvider {
    private val similarity = JaroWinklerSimilarity()

    override fun providerName(): Provider {
        return YEN_PRESS
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getBook(YenPressBookId(seriesId.id))
        val thumbnail = client.getBookThumbnail(series)

        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(YenPressBookId(bookId.id))
        val thumbnail = client.getBookThumbnail(bookMetadata)

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400)).take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(seriesName.take(400))
        val match = bestMatch(seriesName, searchResults)?.let { client.getBook(it.id) }

        return match?.let {
            val thumbnail = client.getBookThumbnail(it)
            return metadataMapper.toSeriesMetadata(it, thumbnail)
        }
    }

    private fun bestMatch(name: String, searchResults: Collection<YenPressSearchResult>): YenPressSearchResult? {
        return searchResults
            .map { Pair(similarity.apply(name.uppercase(), it.title.uppercase()), it) }
            .filter { (score, _) -> score > 0.9 }
            .maxByOrNull { (score, _) -> score }
            ?.second
    }
}
