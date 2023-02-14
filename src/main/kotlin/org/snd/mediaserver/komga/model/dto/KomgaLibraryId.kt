package org.snd.mediaserver.komga.model.dto

import org.snd.mediaserver.model.mediaserver.MediaServerLibraryId

@JvmInline
value class KomgaLibraryId(val id: String)

fun MediaServerLibraryId.komgaLibraryId() = KomgaLibraryId(id)