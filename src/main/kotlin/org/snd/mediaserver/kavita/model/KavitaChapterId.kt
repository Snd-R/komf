package org.snd.mediaserver.kavita.model

import org.snd.mediaserver.model.MediaServerBookId

@JvmInline
value class KavitaChapterId(val id: Int)

fun MediaServerBookId.kavitaChapterId() = KavitaChapterId(id.toInt())
