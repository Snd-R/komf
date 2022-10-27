package org.snd.mediaserver.komga.model.dto

import org.snd.mediaserver.model.MediaServerLibraryId

@JvmInline
value class KomgaLibraryId(val id: String)

fun MediaServerLibraryId.komgaLibraryId() = KomgaLibraryId(id)