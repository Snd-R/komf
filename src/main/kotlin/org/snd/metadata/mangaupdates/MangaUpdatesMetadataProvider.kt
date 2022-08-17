package org.snd.metadata.mangaupdates

import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.snd.metadata.MetadataProvider
import org.snd.metadata.mangaupdates.model.SearchResult
import org.snd.metadata.mangaupdates.model.toSeriesSearchResult
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.MANGA_UPDATES
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesSearchResult

class MangaUpdatesMetadataProvider(
    private val client: MangaUpdatesClient,
    private val metadataMapper: MangaUpdatesMetadataMapper,
) : MetadataProvider {
    private val similarity = JaroWinklerSimilarity()

    override fun providerName(): Provider {
        return MANGA_UPDATES
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(seriesId.id.toLong())
        val thumbnail = client.getThumbnail(series)
        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400), 1, limit).results.take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(seriesName.take(400)).results
        val match = bestMatch(seriesName, searchResults)?.let { client.getSeries(it.id) }

        return match?.let {
            val thumbnail = client.getThumbnail(it)
            return metadataMapper.toSeriesMetadata(it, thumbnail)
        }
    }

    private fun bestMatch(name: String, searchResults: Collection<SearchResult>): SearchResult? {
        return searchResults
            .map { Pair(similarity.apply(name.uppercase(), it.title.uppercase()), it) }
            .filter { (score, _) -> score > 0.9 }
            .maxByOrNull { (score, _) -> score }
            ?.second
    }
}
