package org.snd.mediaserver.repository

import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServerBookId
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.mediaserver.model.MediaServerThumbnailId

data class BookThumbnail(
    val bookId: MediaServerBookId,
    val type: MediaServer,
    val seriesId: MediaServerSeriesId,
    val thumbnailId: MediaServerThumbnailId?,
)
