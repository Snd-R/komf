package org.snd.mediaserver.model.mediaserver

data class MediaServerSeriesSearch(
    val id: MediaServerSeriesId,
    val libraryId: MediaServerLibraryId,
    val name: String,
)