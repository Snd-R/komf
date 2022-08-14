package org.snd.metadata.anilist

import kotlinx.coroutines.runBlocking
import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.snd.SearchQuery
import org.snd.fragment.AniListManga
import org.snd.metadata.MetadataProvider
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesSearchResult

class AniListMetadataProvider(
    private val client: AniListClient,
    private val metadataMapper: AniListMetadataMapper,
) : MetadataProvider {
    private val similarity = JaroWinklerSimilarity()

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): SeriesMetadata {
        val series = client.getMedia(seriesId.id.toInt())
        val thumbnail = client.getThumbnail(series.aniListManga)
        return metadataMapper.toSeriesMetadata(series.aniListManga, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): BookMetadata? {
        return null
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = runBlocking {
            client.search(seriesName.take(400), limit)
        }
        return searchResults.map { metadataMapper.toSearchResult(it.aniListManga) }
    }

    override fun matchSeriesMetadata(seriesName: String): SeriesMetadata? {
        val searchResults = client.search(seriesName.take(400))
        val match = bestMatch(seriesName, searchResults)

        return match?.let {
            val thumbnail = client.getThumbnail(it.aniListManga)
            metadataMapper.toSeriesMetadata(it.aniListManga, thumbnail)
        }
    }

    private fun bestMatch(name: String, searchResults: Collection<SearchQuery.Medium>): SearchQuery.Medium? {
        return searchResults
            .map { Pair(getSimilarity(name, it.aniListManga), it) }
            .filter { (score, _) -> score > 0.9 }
            .maxByOrNull { (score, _) -> score }
            ?.second
    }

    private fun getSimilarity(name: String, searchResult: AniListManga): Double {
        val titles = listOfNotNull(
            searchResult.title?.english,
            searchResult.title?.romaji,
            searchResult.title?.native,
        )

        return titles.maxOf { similarity.apply(name.uppercase(), it.uppercase()) }
    }
}
