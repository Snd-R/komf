package org.snd.mediaserver.komga.model.dto

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.model.MediaServerBookId
import org.snd.mediaserver.model.MediaServerBookThumbnail
import org.snd.mediaserver.model.MediaServerThumbnailId

@JsonClass(generateAdapter = true)
data class KomgaBookThumbnail(
    val id: String,
    val bookId: String,
    val type: String,
    val selected: Boolean,
)

fun KomgaBookThumbnail.mediaServerBookThumbnail() = MediaServerBookThumbnail(
    id = MediaServerThumbnailId(id),
    bookId = MediaServerBookId(bookId),
    type = type,
    selected = selected,
)