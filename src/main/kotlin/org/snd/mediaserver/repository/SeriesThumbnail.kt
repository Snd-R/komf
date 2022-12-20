package org.snd.mediaserver.repository

import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.mediaserver.model.MediaServerThumbnailId

data class SeriesThumbnail(
    val thumbnailId: MediaServerThumbnailId?,
    val seriesId: MediaServerSeriesId,
)
