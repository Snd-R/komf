package org.snd.metadata.mangaupdates

import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.snd.metadata.MetadataProvider
import org.snd.metadata.mangaupdates.model.SearchResult
import org.snd.metadata.mangaupdates.model.toSeriesMetadata
import org.snd.metadata.mangaupdates.model.toSeriesSearchResult
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesSearchResult

class MangaUpdatesMetadataProvider(
    private val client: MangaUpdatesClient,
) : MetadataProvider {
    private val similarity = JaroWinklerSimilarity()

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): SeriesMetadata {
        val series = client.getSeries(seriesId.id.toLong())
        val thumbnail = client.getThumbnail(series)
        return series.toSeriesMetadata(thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): BookMetadata? {
        return null
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400), 1, limit).results.take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): SeriesMetadata? {
        val searchResults = client.searchSeries(seriesName.take(400)).results
        val match = bestMatch(seriesName, searchResults)?.let { client.getSeries(it.id) }

        return match?.let {
            val thumbnail = client.getThumbnail(it)
            it.toSeriesMetadata(thumbnail)
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
