package org.snd.mediaserver.model

import org.snd.mediaserver.model.mediaserver.MediaServerBookId
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId
import org.snd.mediaserver.model.mediaserver.MediaServerThumbnailId

data class BookThumbnail(
    val bookId: MediaServerBookId,
    val seriesId: MediaServerSeriesId,
    val thumbnailId: MediaServerThumbnailId?,
)
