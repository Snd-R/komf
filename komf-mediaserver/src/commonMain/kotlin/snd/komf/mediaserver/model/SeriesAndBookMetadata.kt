package snd.komf.mediaserver.model

import snd.komf.model.BookMetadata
import snd.komf.model.SeriesMetadata

data class SeriesAndBookMetadata(
    val seriesMetadata: SeriesMetadata,
    val bookMetadata: Map<MediaServerBook, BookMetadata?>,
)
