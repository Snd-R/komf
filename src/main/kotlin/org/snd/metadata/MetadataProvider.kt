package org.snd.metadata

import org.snd.metadata.model.Provider
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesMatchResult
import org.snd.metadata.model.SeriesSearchResult

interface MetadataProvider {
    fun providerName(): Provider

    fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata

    fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata

    fun searchSeries(seriesName: String, limit: Int = 5): Collection<SeriesSearchResult>

    fun matchSeriesMetadata(seriesName: String): SeriesMatchResult
}
