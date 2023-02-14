package org.snd.mediaserver.model.mediaserver

data class MediaServerLibrary(
    val id: MediaServerLibraryId,
    val name: String,
    val roots: Collection<String>,
)
