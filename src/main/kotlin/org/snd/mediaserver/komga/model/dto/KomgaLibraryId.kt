package org.snd.mediaserver.komga.model.dto

import org.snd.mediaserver.model.MediaServerLibraryId

data class KomgaLibraryId(val id: String)

fun MediaServerLibraryId.komgaLibraryId() = KomgaLibraryId(id)