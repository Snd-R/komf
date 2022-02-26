package org.snd.metadata.mal

import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.snd.metadata.MetadataProvider
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.mal.model.SearchResult
import org.snd.metadata.mal.model.SearchResults
import org.snd.metadata.mal.model.toSeriesMetadata
import org.snd.metadata.mal.model.toSeriesSearchResult
import org.snd.model.SeriesMetadata
import org.snd.model.SeriesSearchResult


class MalMetadataProvider(
    private val malClient: MalClient,
) : MetadataProvider {
    private val similarity = JaroWinklerSimilarity()

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): SeriesMetadata {
        val series = malClient.getSeries(seriesId.id.toInt())
        val thumbnail = malClient.getThumbnail(series)

        return series.toSeriesMetadata(thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        return malClient.searchSeries(seriesName).results.take(limit)
            .map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): SeriesMetadata? {
        val searchResults = malClient.searchSeries(seriesName)
        val match = bestMatch(seriesName, searchResults)?.let { malClient.getSeries(it.id) }

        return match?.let {
            val thumbnail = malClient.getThumbnail(it)
            it.toSeriesMetadata(thumbnail)
        }
    }

    private fun bestMatch(name: String, searchResults: SearchResults): SearchResult? {
        return searchResults.results
            .map { Pair(getSimilarity(name, it), it) }
            .filter { (score, _) -> score > 0.9 }
            .maxByOrNull { (score, _) -> score }
            ?.second
    }

    private fun getSimilarity(name: String, searchResult: SearchResult): Double {
        val titles = listOf(
            searchResult.title,
            searchResult.alternative_titles.en,
            searchResult.alternative_titles.ja,
        ) + searchResult.alternative_titles.synonyms

        return titles.maxOf { similarity.apply(name, it) }
    }
}
