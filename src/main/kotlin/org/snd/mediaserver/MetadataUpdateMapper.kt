package org.snd.mediaserver

import org.snd.mediaserver.model.MediaServerAuthor
import org.snd.mediaserver.model.MediaServerBook
import org.snd.mediaserver.model.MediaServerBookMetadataUpdate
import org.snd.mediaserver.model.MediaServerSeriesMetadata
import org.snd.mediaserver.model.MediaServerSeriesMetadataUpdate
import org.snd.metadata.comicinfo.model.AgeRating
import org.snd.metadata.comicinfo.model.ComicInfo
import org.snd.metadata.model.AuthorRole.COLORIST
import org.snd.metadata.model.AuthorRole.COVER
import org.snd.metadata.model.AuthorRole.EDITOR
import org.snd.metadata.model.AuthorRole.INKER
import org.snd.metadata.model.AuthorRole.LETTERER
import org.snd.metadata.model.AuthorRole.PENCILLER
import org.snd.metadata.model.AuthorRole.TRANSLATOR
import org.snd.metadata.model.AuthorRole.WRITER
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.SeriesMetadata

class MetadataUpdateMapper {

    fun toBookMetadataUpdate(
        bookMetadata: BookMetadata?,
        seriesMetadata: SeriesMetadata?,
        book: MediaServerBook
    ): MediaServerBookMetadataUpdate {
        return with(book.metadata) {
            val authors = (bookMetadata?.authors?.ifEmpty { seriesMetadata?.authors } ?: seriesMetadata?.authors?.ifEmpty { null })
                ?.map { author -> MediaServerAuthor(author.name, author.role.name) }

            MediaServerBookMetadataUpdate(
                summary = getIfNotLockedOrEmpty(bookMetadata?.summary, summaryLock),
                releaseDate = getIfNotLockedOrEmpty(bookMetadata?.releaseDate, releaseDateLock),
                authors = getIfNotLockedOrEmpty(authors, authorsLock),
                tags = getIfNotLockedOrEmpty(bookMetadata?.tags, tagsLock),
                isbn = getIfNotLockedOrEmpty(bookMetadata?.isbn, isbnLock),
                number = getIfNotLockedOrEmpty(bookMetadata?.number?.toString(), numberLock),
                numberSort = getIfNotLockedOrEmpty(bookMetadata?.number?.start, numberSortLock),
            )
        }
    }

    fun toSeriesMetadataUpdate(patch: SeriesMetadata, metadata: MediaServerSeriesMetadata): MediaServerSeriesMetadataUpdate =
        with(metadata) {
            val authors = (patch.authors.map { MediaServerAuthor(it.name, it.role.name) }.ifEmpty { null })

            MediaServerSeriesMetadataUpdate(
                status = getIfNotLockedOrEmpty(patch.status, statusLock),
                title = getIfNotLockedOrEmpty(patch.title, titleLock),
                titleSort = getIfNotLockedOrEmpty(patch.title, titleSortLock),
                alternativeTitles = getIfNotLockedOrEmpty(patch.titles.filter { it != patch.title }, titleSortLock),
                summary = getIfNotLockedOrEmpty(patch.summary, summaryLock),
                publisher = getIfNotLockedOrEmpty(patch.publisher, publisherLock),
                alternativePublishers = getIfNotLockedOrEmpty(patch.alternativePublishers, publisherLock),
                readingDirection = getIfNotLockedOrEmpty(patch.readingDirection, readingDirectionLock),
                ageRating = getIfNotLockedOrEmpty(patch.ageRating, ageRatingLock),
                language = getIfNotLockedOrEmpty(patch.language, languageLock),
                genres = getIfNotLockedOrEmpty(patch.genres, genresLock),
                tags = getIfNotLockedOrEmpty(patch.tags, tagsLock),
                totalBookCount = getIfNotLockedOrEmpty(patch.totalBookCount, totalBookCountLock),
                authors = getIfNotLockedOrEmpty(authors, authorsLock),
                releaseYear = getIfNotLockedOrEmpty(patch.releaseDate?.year, releaseYearLock)
            )
        }

    fun toComicInfo(bookMetadata: BookMetadata?, seriesMetadata: SeriesMetadata?): ComicInfo? {
        if (bookMetadata == null && seriesMetadata == null) return null
        val authors = (bookMetadata?.authors?.ifEmpty { seriesMetadata?.authors }) ?: seriesMetadata?.authors

        return ComicInfo(
            series = seriesMetadata?.title?.name,
            number = bookMetadata?.number?.toString(),
            count = seriesMetadata?.totalBookCount,
            summary = bookMetadata?.summary,
            year = bookMetadata?.releaseDate?.year,
            month = bookMetadata?.releaseDate?.monthValue,
            day = bookMetadata?.releaseDate?.dayOfMonth,
            writer = authors?.filter { it.role == WRITER }?.joinToString(",") { it.name },
            penciller = authors?.filter { it.role == PENCILLER }?.joinToString(",") { it.name },
            inker = authors?.filter { it.role == INKER }?.joinToString(",") { it.name },
            colorist = authors?.filter { it.role == COLORIST }?.joinToString(",") { it.name },
            letterer = authors?.filter { it.role == LETTERER }?.joinToString(",") { it.name },
            coverArtist = authors?.filter { it.role == COVER }?.joinToString(",") { it.name },
            editor = authors?.filter { it.role == EDITOR }?.joinToString(",") { it.name },
            translator = authors?.filter { it.role == TRANSLATOR }?.joinToString(",") { it.name },
            publisher = seriesMetadata?.publisher,
            genre = seriesMetadata?.genres?.joinToString(","),
            tags = bookMetadata?.tags?.joinToString(","),
            ageRating = seriesMetadata?.ageRating
                ?.let { metadataRating ->
                    (AgeRating.values()
                        .filter { it.ageRating != null }
                        .sortedBy { it.ageRating }
                        .firstOrNull { it.ageRating == it.ageRating!!.coerceAtLeast(metadataRating) }
                        ?: AgeRating.ADULTS_ONLY_18)
                        .value
                },
            languageISO = seriesMetadata?.language
        )
    }

    fun toSeriesComicInfo(seriesMetadata: SeriesMetadata): ComicInfo {
        val authors = seriesMetadata.authors
        return ComicInfo(
            series = seriesMetadata.title?.name,
            count = seriesMetadata.totalBookCount,
            summary = seriesMetadata.summary,
            year = seriesMetadata.releaseDate?.year,
            month = seriesMetadata.releaseDate?.month,
            day = seriesMetadata.releaseDate?.day,
            writer = authors.filter { it.role == WRITER }.joinToString(",") { it.name },
            penciller = authors.filter { it.role == PENCILLER }.joinToString(",") { it.name },
            inker = authors.filter { it.role == INKER }.joinToString(",") { it.name },
            colorist = authors.filter { it.role == COLORIST }.joinToString(",") { it.name },
            letterer = authors.filter { it.role == LETTERER }.joinToString(",") { it.name },
            coverArtist = authors.filter { it.role == COVER }.joinToString(",") { it.name },
            editor = authors.filter { it.role == EDITOR }.joinToString(",") { it.name },
            translator = authors.filter { it.role == TRANSLATOR }.joinToString(",") { it.name },
            publisher = seriesMetadata.publisher,
            genre = seriesMetadata.genres.joinToString(","),
            tags = seriesMetadata.tags.joinToString(","),
            ageRating = seriesMetadata.ageRating
                ?.let { metadataRating ->
                    (AgeRating.values()
                        .filter { it.ageRating != null }
                        .sortedBy { it.ageRating }
                        .firstOrNull { it.ageRating == it.ageRating!!.coerceAtLeast(metadataRating) }
                        ?: AgeRating.ADULTS_ONLY_18)
                        .value
                },
            languageISO = seriesMetadata.language
        )
    }

    private fun <T> getIfNotLockedOrEmpty(patched: T?, lock: Boolean): T? =
        if (patched is Collection<*> && patched.isEmpty()) null
        else if (patched != null && !lock) patched
        else null
}
