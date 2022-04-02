package org.snd.komga.model

import org.snd.komga.model.dto.KomgaBookId
import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.komga.model.dto.KomgaThumbnailId

data class MatchedBook(
    val bookId: KomgaBookId,
    val seriesId: KomgaSeriesId,
    val thumbnailId: KomgaThumbnailId?,
)