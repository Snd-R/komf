package org.snd.metadata.nautiljon

import org.apache.commons.text.similarity.JaroWinklerSimilarity
import org.snd.metadata.MetadataProvider
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.nautiljon.model.SearchResult
import org.snd.metadata.nautiljon.model.SeriesId
import org.snd.metadata.nautiljon.model.VolumeId
import org.snd.metadata.nautiljon.model.toSeriesSearchResult

class NautiljonMetadataProvider(
    private val client: NautiljonClient,
    private val metadataMapper: NautiljonSeriesMetadataMapper,
) : MetadataProvider {
    private val similarity = JaroWinklerSimilarity()

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): SeriesMetadata {
        val series = client.getSeries(SeriesId(seriesId.id))
        val thumbnail = client.getSeriesThumbnail(series)

        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): BookMetadata? {
        val bookMetadata = client.getBook(SeriesId(seriesId.id), VolumeId(bookId.id))
        val thumbnail = client.getVolumeThumbnail(bookMetadata)

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400)).take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): SeriesMetadata? {
        val searchResults = client.searchSeries(seriesName.take(400))
        val match = bestMatch(seriesName, searchResults)?.let { client.getSeries(it.id) }

        return match?.let {
            val thumbnail = client.getSeriesThumbnail(it)
            metadataMapper.toSeriesMetadata(it, thumbnail)
        }
    }

    private fun bestMatch(name: String, searchResults: Collection<SearchResult>): SearchResult? {
        return searchResults
            .map { Pair(getSimilarity(name, it), it) }
            .filter { (score, _) -> score > 0.9 }
            .maxByOrNull { (score, _) -> score }
            ?.second
    }

    private fun getSimilarity(name: String, searchResult: SearchResult): Double {
        val titles = listOfNotNull(
            searchResult.title.uppercase(),
            searchResult.alternativeTitle?.uppercase()
        )

        return titles.maxOf { similarity.apply(name.uppercase(), it) }
    }

}
