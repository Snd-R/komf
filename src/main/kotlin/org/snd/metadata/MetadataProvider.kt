package org.snd.metadata

import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata

interface MetadataProvider {
    fun providerName(): Provider

    fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata

    fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata

    fun searchSeries(seriesName: String, limit: Int = 5): Collection<SeriesSearchResult>

    fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata?
}
