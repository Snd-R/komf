package org.snd.mediaserver.model.mediaserver

data class MediaServerSeriesThumbnail(
    val id: MediaServerThumbnailId,
    val seriesId: MediaServerSeriesId,
    val type: String?,
    val selected: Boolean,
)
