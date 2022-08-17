package org.snd.metadata

import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.SeriesMetadata

object MetadataMerger {
    fun mergeSeriesMetadata(
        originalSeriesMetadata: SeriesMetadata,
        newSeriesMetadata: SeriesMetadata,
    ): SeriesMetadata {
        return SeriesMetadata(
            status = originalSeriesMetadata.status ?: newSeriesMetadata.status,
            title = originalSeriesMetadata.title ?: newSeriesMetadata.title,
            titleSort = originalSeriesMetadata.titleSort ?: newSeriesMetadata.titleSort,
            summary = originalSeriesMetadata.summary ?: newSeriesMetadata.summary,
            publisher = originalSeriesMetadata.publisher ?: newSeriesMetadata.publisher,
            readingDirection = originalSeriesMetadata.readingDirection ?: newSeriesMetadata.readingDirection,
            ageRating = originalSeriesMetadata.ageRating ?: newSeriesMetadata.ageRating,
            language = originalSeriesMetadata.language ?: newSeriesMetadata.language,
            genres = originalSeriesMetadata.genres ?: newSeriesMetadata.genres,
            tags = originalSeriesMetadata.tags ?: newSeriesMetadata.tags,
            totalBookCount = originalSeriesMetadata.totalBookCount ?: newSeriesMetadata.totalBookCount,
            authors = originalSeriesMetadata.authors ?: newSeriesMetadata.authors,
            thumbnail = originalSeriesMetadata.thumbnail ?: newSeriesMetadata.thumbnail,
        )
    }

    fun mergeBookMetadata(
        originalBookMetadata: Map<String, BookMetadata?>,
        newBookMetadata: Map<String, BookMetadata?>,
    ): Map<String, BookMetadata?> = (originalBookMetadata.asSequence() + newBookMetadata.asSequence()).distinct()
        .groupBy({ it.key }, { it.value })
        .mapValues { (_, values) -> mergeBookMetadata(values) }

    private fun mergeBookMetadata(metadata: Collection<BookMetadata?>): BookMetadata? {
        return metadata.filterNotNull().reduceOrNull { a, b ->
            BookMetadata(
                title = a.title ?: b.title,
                summary = a.summary ?: b.summary,
                number = a.number ?: b.number,
                numberSort = a.numberSort ?: b.numberSort,
                releaseDate = a.releaseDate ?: b.releaseDate,
                authors = a.authors ?: b.authors,
                tags = a.tags ?: b.tags,
                isbn = a.isbn ?: b.isbn,
                links = a.links ?: b.links,
                chapters = a.chapters ?: b.chapters,
                startChapter = a.startChapter ?: b.startChapter,
                endChapter = a.endChapter ?: b.endChapter,
                thumbnail = a.thumbnail ?: b.thumbnail,
            )
        }
    }
}
