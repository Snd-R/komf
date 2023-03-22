package org.snd.mediaserver.model.mediaserver

data class MediaServerSeries(
    val id: MediaServerSeriesId,
    val libraryId: MediaServerLibraryId,
    val name: String,
    val booksCount: Int,
    val metadata: MediaServerSeriesMetadata,
    val url: String,
    val deleted: Boolean,
)
