package org.snd.mediaserver.model

data class MediaServerSeriesSearch(
    val id: MediaServerSeriesId,
    val libraryId: MediaServerLibraryId,
    val name: String,
)