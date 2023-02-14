package org.snd.mediaserver.model.mediaserver

data class MediaServerBookThumbnail(
    val id: MediaServerThumbnailId,
    val bookId: MediaServerBookId,
    val type: String?,
    val selected: Boolean,
)

