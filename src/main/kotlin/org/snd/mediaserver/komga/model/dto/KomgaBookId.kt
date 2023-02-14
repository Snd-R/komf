package org.snd.mediaserver.komga.model.dto

import org.snd.mediaserver.model.mediaserver.MediaServerBookId

@JvmInline
value class KomgaBookId(val id: String)

fun MediaServerBookId.komgaBookId() = KomgaBookId(id)
