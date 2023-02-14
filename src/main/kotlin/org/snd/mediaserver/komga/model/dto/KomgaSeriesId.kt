package org.snd.mediaserver.komga.model.dto

import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId

@JvmInline
value class KomgaSeriesId(val id: String)

fun MediaServerSeriesId.komgaSeriesId() = KomgaSeriesId(id)
