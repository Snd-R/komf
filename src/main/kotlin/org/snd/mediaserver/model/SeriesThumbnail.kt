package org.snd.mediaserver.model

import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId
import org.snd.mediaserver.model.mediaserver.MediaServerThumbnailId

data class SeriesThumbnail(
    val thumbnailId: MediaServerThumbnailId?,
    val seriesId: MediaServerSeriesId,
)
