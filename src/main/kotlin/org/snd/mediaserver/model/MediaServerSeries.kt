package org.snd.mediaserver.model

data class MediaServerSeries(
    val id: MediaServerSeriesId,
    val libraryId: MediaServerLibraryId,
    val name: String,
    val metadata: MediaServerSeriesMetadata,
    val deleted: Boolean,
)
