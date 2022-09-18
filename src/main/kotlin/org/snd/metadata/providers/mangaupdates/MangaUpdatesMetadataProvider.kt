package org.snd.metadata.providers.mangaupdates

import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.MANGA_UPDATES
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesMatchResult
import org.snd.metadata.model.SeriesMatchStatus.MATCHED
import org.snd.metadata.model.SeriesMatchStatus.NO_MATCH
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.providers.mangaupdates.model.toSeriesSearchResult

class MangaUpdatesMetadataProvider(
    private val client: MangaUpdatesClient,
    private val metadataMapper: MangaUpdatesMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
) : MetadataProvider {

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

    override fun matchSeriesMetadata(seriesName: String): SeriesMatchResult {
        val searchResults = client.searchSeries(seriesName.take(400)).results

        val metadata = searchResults
            .firstOrNull { nameMatcher.matches(seriesName, it.title) }
            ?.let {
                val series = client.getSeries(it.id)
                val thumbnail = client.getThumbnail(series)
                metadataMapper.toSeriesMetadata(series, thumbnail)
            }

        return SeriesMatchResult(
            status = if (metadata == null) NO_MATCH else MATCHED,
            result = metadata
        )
    }
}
