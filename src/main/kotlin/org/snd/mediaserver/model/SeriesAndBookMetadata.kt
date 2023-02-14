package org.snd.mediaserver.model

import org.snd.mediaserver.model.mediaserver.MediaServerBook
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.SeriesMetadata

data class SeriesAndBookMetadata(
    val seriesMetadata: SeriesMetadata,
    val bookMetadata: Map<MediaServerBook, BookMetadata?>,
)
