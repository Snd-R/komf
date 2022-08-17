package org.snd.komga.model

import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.komga.model.dto.KomgaThumbnailId

data class MatchedSeries(
    val seriesId: KomgaSeriesId,
    val thumbnailId: KomgaThumbnailId?,
)
