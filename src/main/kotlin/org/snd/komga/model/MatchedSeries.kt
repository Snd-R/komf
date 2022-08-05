package org.snd.komga.model

import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.komga.model.dto.KomgaThumbnailId
import org.snd.metadata.Provider
import org.snd.metadata.model.ProviderSeriesId

data class MatchedSeries(
    val seriesId: KomgaSeriesId,
    val thumbnailId: KomgaThumbnailId?,
    val provider: Provider,
    val providerSeriesId: ProviderSeriesId
)
