package org.snd.mediaserver.komga.model.dto

import org.snd.mediaserver.model.MediaServerSeriesId

@JvmInline
value class KomgaSeriesId(val id: String)

fun MediaServerSeriesId.komgaSeriesId() = KomgaSeriesId(id)
