package org.snd.mediaserver.komga.model.dto

import org.snd.mediaserver.model.MediaServerThumbnailId

@JvmInline
value class KomgaThumbnailId(val id: String)

fun MediaServerThumbnailId.komgaThumbnailId() = KomgaThumbnailId(id)
