package snd.komf.mediaserver.match

import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerThumbnailId

data class BookThumbnailMatch(
    val bookId: MediaServerBookId,
    val seriesId: MediaServerSeriesId,
    val thumbnailId: MediaServerThumbnailId,
    val mediaServer: MediaServer
)