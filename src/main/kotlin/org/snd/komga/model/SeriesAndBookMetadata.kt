package org.snd.komga.model

import org.snd.komga.model.dto.KomgaBook
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.SeriesMetadata

data class SeriesAndBookMetadata(
    val seriesMetadata: SeriesMetadata?,
    val bookMetadata: Map<KomgaBook, BookMetadata>?
)
