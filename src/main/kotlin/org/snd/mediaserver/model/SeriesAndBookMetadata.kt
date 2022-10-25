package org.snd.mediaserver.model

import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.SeriesMetadata

data class SeriesAndBookMetadata(
    val seriesMetadata: SeriesMetadata?,
    val bookMetadata: Map<MediaServerBook, BookMetadata?>
)
