package org.snd.mediaserver.kavita.model

import org.snd.mediaserver.model.MediaServerSeriesId

@JvmInline
value class KavitaSeriesId(val id: Int)

fun MediaServerSeriesId.kavitaSeriesId() = KavitaSeriesId(id.toInt())
