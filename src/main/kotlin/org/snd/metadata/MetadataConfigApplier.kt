package org.snd.metadata

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.SeriesMetadata

object MetadataConfigApplier {

    fun apply(metadata: SeriesMetadata, config: SeriesMetadataConfig): SeriesMetadata {
        return with(metadata) {
            SeriesMetadata(
                id = id,
                provider = provider,
                status = getIfEnabled(status, config.status),
                title = getIfEnabled(title, config.title),
                titleSort = getIfEnabled(titleSort, config.titleSort),
                summary = getIfEnabled(summary, config.summary),
                publisher = getIfEnabled(publisher, config.publisher),
                readingDirection = getIfEnabled(readingDirection, config.readingDirection),
                ageRating = getIfEnabled(ageRating, config.ageRating),
                language = getIfEnabled(language, config.language),
                genres = getIfEnabled(genres, config.genres),
                tags = getIfEnabled(tags, config.tags),
                totalBookCount = getIfEnabled(totalBookCount, config.totalBookCount),
                authors = getIfEnabled(authors, config.authors),
                thumbnail = getIfEnabled(thumbnail, config.thumbnail),
                books = getIfEnabled(books, config.books) ?: emptyList(),
            )
        }
    }

    fun apply(metadata: BookMetadata, config: BookMetadataConfig): BookMetadata {
        return with(metadata) {
            BookMetadata(
                id = id,
                title = getIfEnabled(title, config.title),
                summary = getIfEnabled(summary, config.summary),
                number = getIfEnabled(number, config.number),
                numberSort = getIfEnabled(numberSort, config.numberSort),
                releaseDate = getIfEnabled(releaseDate, config.releaseDate),
                authors = getIfEnabled(authors, config.authors),
                tags = getIfEnabled(tags, config.tags),
                isbn = getIfEnabled(isbn, config.isbn),
                links = getIfEnabled(links, config.links),
                thumbnail = getIfEnabled(thumbnail, config.thumbnail),
                chapters = chapters,
                startChapter = startChapter,
                endChapter = endChapter,
            )
        }
    }

    private fun <T> getIfEnabled(value: T?, enabled: Boolean): T? =
        if (value != null && enabled) value
        else null
}
