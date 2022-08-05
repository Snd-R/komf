package org.snd.metadata

import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesSearchResult

interface MetadataProvider {
    fun getSeriesMetadata(seriesId: ProviderSeriesId): SeriesMetadata

    fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): BookMetadata?

    fun searchSeries(seriesName: String, limit: Int = 5): Collection<SeriesSearchResult>

    fun matchSeriesMetadata(seriesName: String): SeriesMetadata?
}
