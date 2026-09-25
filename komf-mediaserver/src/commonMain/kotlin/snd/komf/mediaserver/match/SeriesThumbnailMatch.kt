package snd.komf.mediaserver.match

import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerThumbnailId

data class SeriesThumbnailMatch(
    val seriesId: MediaServerSeriesId,
    val thumbnailId: MediaServerThumbnailId,
    val mediaServer: MediaServer
)