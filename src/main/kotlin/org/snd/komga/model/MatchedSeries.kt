package org.snd.komga.model

import org.snd.metadata.Provider
import org.snd.metadata.ProviderSeriesId

data class MatchedSeries(
    val seriesId: SeriesId,
    val thumbnailId: ThumbnailId?,
    val provider: Provider,
    val providerSeriesId: ProviderSeriesId
)
