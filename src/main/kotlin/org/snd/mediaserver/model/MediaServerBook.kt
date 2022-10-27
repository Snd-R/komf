package org.snd.mediaserver.model

data class MediaServerBook(
    val id: MediaServerBookId,
    val seriesId: MediaServerSeriesId,
    val libraryId: MediaServerLibraryId?,
    val seriesTitle: String,
    val name: String,
    val url: String,
    val number: Int,
    val metadata: MediaServerBookMetadata,
    val deleted: Boolean,
)
