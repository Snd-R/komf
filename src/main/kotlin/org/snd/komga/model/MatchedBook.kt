package org.snd.komga.model

import org.snd.komga.model.dto.BookId
import org.snd.komga.model.dto.SeriesId
import org.snd.komga.model.dto.ThumbnailId

data class MatchedBook(
    val bookId: BookId,
    val seriesId: SeriesId,
    val thumbnailId: ThumbnailId?,
)