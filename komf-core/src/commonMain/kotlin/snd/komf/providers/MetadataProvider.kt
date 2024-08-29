package snd.komf.providers

import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult

interface MetadataProvider {
    fun providerName(): CoreProviders

    suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata

    suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image?

    suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata

    suspend fun searchSeries(seriesName: String, limit: Int = 5): Collection<SeriesSearchResult>

    suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata?
}

