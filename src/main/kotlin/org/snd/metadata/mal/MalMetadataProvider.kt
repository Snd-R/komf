package org.snd.metadata.mal

import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.snd.metadata.MetadataProvider
import org.snd.metadata.mal.model.SearchResult
import org.snd.metadata.mal.model.SearchResults
import org.snd.metadata.mal.model.toSeriesSearchResult
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.MAL
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesSearchResult


class MalMetadataProvider(
    private val malClient: MalClient,
    private val metadataMapper: MalMetadataMapper,
) : MetadataProvider {
    private val similarity = JaroWinklerSimilarity()

    override fun providerName(): Provider {
        return MAL
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = malClient.getSeries(seriesId.id.toInt())
        val thumbnail = malClient.getThumbnail(series)

        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        return malClient.searchSeries(seriesName.take(64)).results.take(limit)
            .map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = malClient.searchSeries(seriesName.take(64))
        val match = bestMatch(seriesName, searchResults)?.let { malClient.getSeries(it.id) }

        return match?.let {
            val thumbnail = malClient.getThumbnail(it)
            metadataMapper.toSeriesMetadata(it, thumbnail)
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
            searchResult.title.uppercase(),
            searchResult.alternative_titles.en.uppercase(),
            searchResult.alternative_titles.ja.uppercase(),
        ) + searchResult.alternative_titles.synonyms.map { it.uppercase() }

        return titles.maxOf { similarity.apply(name.uppercase(), it) }
    }
}
