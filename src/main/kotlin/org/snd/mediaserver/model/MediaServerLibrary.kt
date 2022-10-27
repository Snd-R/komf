package org.snd.mediaserver.model

data class MediaServerLibrary(
    val id: MediaServerLibraryId,
    val name: String,
    val roots: Collection<String>,
)
