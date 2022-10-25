package org.snd.mediaserver.komga.model.dto

import org.snd.mediaserver.model.MediaServerBookId

data class KomgaBookId(val id: String)

fun MediaServerBookId.komgaBookId() = KomgaBookId(id)
