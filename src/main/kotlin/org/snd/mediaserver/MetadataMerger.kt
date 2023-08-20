package org.snd.mediaserver

import org.snd.mediaserver.model.mediaserver.MediaServerBookId
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.SeriesMetadata

class MetadataMerger(
    private val mergeTags: Boolean,
    private val mergeGenres: Boolean,
) {
    fun mergeSeriesMetadata(
        originalSeriesMetadata: SeriesMetadata,
        newSeriesMetadata: SeriesMetadata,
    ): SeriesMetadata {
        return SeriesMetadata(
            status = originalSeriesMetadata.status ?: newSeriesMetadata.status,
            titles = originalSeriesMetadata.titles + newSeriesMetadata.titles,
            summary = originalSeriesMetadata.summary ?: newSeriesMetadata.summary,
            publisher = originalSeriesMetadata.publisher ?: newSeriesMetadata.publisher,
            alternativePublishers = originalSeriesMetadata.alternativePublishers.ifEmpty { newSeriesMetadata.alternativePublishers },
            readingDirection = originalSeriesMetadata.readingDirection ?: newSeriesMetadata.readingDirection,
            ageRating = originalSeriesMetadata.ageRating ?: newSeriesMetadata.ageRating,
            language = originalSeriesMetadata.language ?: newSeriesMetadata.language,
            genres = mergeGenres(originalSeriesMetadata.genres, newSeriesMetadata.genres),
            tags = mergeTags(originalSeriesMetadata.tags, newSeriesMetadata.tags),
            totalBookCount = originalSeriesMetadata.totalBookCount ?: newSeriesMetadata.totalBookCount,
            authors = originalSeriesMetadata.authors.ifEmpty { newSeriesMetadata.authors },
            releaseDate = originalSeriesMetadata.releaseDate ?: newSeriesMetadata.releaseDate,
            links = (originalSeriesMetadata.links + newSeriesMetadata.links).distinctBy { it.label },
            score = originalSeriesMetadata.score ?: newSeriesMetadata.score,
            thumbnail = originalSeriesMetadata.thumbnail ?: newSeriesMetadata.thumbnail,
        )
    }

    fun mergeBookMetadata(
        originalBookMetadata: Map<MediaServerBookId, BookMetadata?>,
        newBookMetadata: Map<MediaServerBookId, BookMetadata?>,
    ): Map<MediaServerBookId, BookMetadata?> = (originalBookMetadata.asSequence() + newBookMetadata.asSequence()).distinct()
        .groupBy({ it.key }, { it.value })
        .mapValues { (_, values) -> mergeBookMetadata(values) }

    private fun mergeBookMetadata(metadata: Collection<BookMetadata?>): BookMetadata? {
        return metadata.filterNotNull().reduceOrNull { old, new ->
            BookMetadata(
                title = old.title ?: new.title,
                summary = old.summary ?: new.summary,
                number = old.number ?: new.number,
                numberSort = old.numberSort ?: new.numberSort,
                releaseDate = old.releaseDate ?: new.releaseDate,
                authors = old.authors.ifEmpty { new.authors },
                tags = old.tags.ifEmpty { new.tags },
                isbn = old.isbn ?: new.isbn,
                links = old.links + new.links,
                chapters = old.chapters.ifEmpty { new.chapters },
                startChapter = old.startChapter ?: new.startChapter,
                endChapter = old.endChapter ?: new.endChapter,
                thumbnail = old.thumbnail ?: new.thumbnail,
            )
        }
    }

    private fun mergeTags(old: Collection<String>, new: Collection<String>): Collection<String> {
        return if (mergeTags) (old + new).toSortedSet { a, b -> a.lowercase().compareTo(b.lowercase()) }
        else old.ifEmpty { new }
    }

    private fun mergeGenres(old: Collection<String>, new: Collection<String>): Collection<String> {
        return if (mergeGenres) (old + new).toSortedSet { a, b -> a.lowercase().compareTo(b.lowercase()) }
        else old.ifEmpty { new }
    }
}
