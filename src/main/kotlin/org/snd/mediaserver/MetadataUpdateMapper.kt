package org.snd.mediaserver

import org.snd.mediaserver.model.mediaserver.MediaServerAuthor
import org.snd.mediaserver.model.mediaserver.MediaServerBook
import org.snd.mediaserver.model.mediaserver.MediaServerBookMetadataUpdate
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesMetadata
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesMetadataUpdate
import org.snd.metadata.comicinfo.model.AgeRating
import org.snd.metadata.comicinfo.model.ComicInfo
import org.snd.metadata.model.metadata.AuthorRole.COLORIST
import org.snd.metadata.model.metadata.AuthorRole.COVER
import org.snd.metadata.model.metadata.AuthorRole.EDITOR
import org.snd.metadata.model.metadata.AuthorRole.INKER
import org.snd.metadata.model.metadata.AuthorRole.LETTERER
import org.snd.metadata.model.metadata.AuthorRole.PENCILLER
import org.snd.metadata.model.metadata.AuthorRole.TRANSLATOR
import org.snd.metadata.model.metadata.AuthorRole.WRITER
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.SeriesMetadata

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
                links = getIfNotLockedOrEmpty(bookMetadata?.links, linksLock),

                number = bookMetadata?.number?.toString(),
                numberSort = bookMetadata?.number?.start,
                numberLock = bookMetadata?.number != null,
                numberSortLock = bookMetadata?.number != null,
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
                releaseYear = getIfNotLockedOrEmpty(patch.releaseDate?.year, releaseYearLock),
                links = getIfNotLockedOrEmpty(patch.links, linksLock),
            )
        }

    fun toComicInfo(bookMetadata: BookMetadata?, seriesMetadata: SeriesMetadata?): ComicInfo? {
        if (bookMetadata == null && seriesMetadata == null) return null
        val authors = ((bookMetadata?.authors?.ifEmpty { seriesMetadata?.authors }) ?: seriesMetadata?.authors)?.ifEmpty { null }

        return ComicInfo(
            series = seriesMetadata?.title?.name,
            number = bookMetadata?.number?.toString(),
            count = seriesMetadata?.totalBookCount,
            summary = bookMetadata?.summary,
            year = bookMetadata?.releaseDate?.year,
            month = bookMetadata?.releaseDate?.monthValue,
            day = bookMetadata?.releaseDate?.dayOfMonth,
            writer = authors?.filter { it.role == WRITER }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            penciller = authors?.filter { it.role == PENCILLER }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            inker = authors?.filter { it.role == INKER }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            colorist = authors?.filter { it.role == COLORIST }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            letterer = authors?.filter { it.role == LETTERER }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            coverArtist = authors?.filter { it.role == COVER }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            editor = authors?.filter { it.role == EDITOR }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            translator = authors?.filter { it.role == TRANSLATOR }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            publisher = seriesMetadata?.publisher,
            genre = seriesMetadata?.genres?.ifEmpty { null }?.joinToString(","),
            tags = bookMetadata?.tags?.ifEmpty { null }?.joinToString(","),
            ageRating = seriesMetadata?.ageRating
                ?.let { metadataRating ->
                    (AgeRating.values()
                        .filter { it.ageRating != null }
                        .sortedBy { it.ageRating }
                        .firstOrNull { it.ageRating == it.ageRating!!.coerceAtLeast(metadataRating) }
                        ?: AgeRating.ADULTS_ONLY_18)
                        .value
                },
            languageISO = seriesMetadata?.language,
            localizedSeries = seriesMetadata?.titles?.find { it.type != null }?.name
        )
    }

    fun toSeriesComicInfo(seriesMetadata: SeriesMetadata, bookMetadata: BookMetadata?): ComicInfo {
        val authors = seriesMetadata.authors.ifEmpty { null }
        return ComicInfo(
            series = seriesMetadata.title?.name,
            number = bookMetadata?.number?.toString(),
            count = seriesMetadata.totalBookCount,
            summary = seriesMetadata.summary,
            year = seriesMetadata.releaseDate?.year,
            month = seriesMetadata.releaseDate?.month,
            day = seriesMetadata.releaseDate?.day,
            writer = authors?.filter { it.role == WRITER }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            penciller = authors?.filter { it.role == PENCILLER }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            inker = authors?.filter { it.role == INKER }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            colorist = authors?.filter { it.role == COLORIST }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            letterer = authors?.filter { it.role == LETTERER }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            coverArtist = authors?.filter { it.role == COVER }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            editor = authors?.filter { it.role == EDITOR }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            translator = authors?.filter { it.role == TRANSLATOR }
                ?.ifEmpty { null }
                ?.joinToString(",") { it.name },
            publisher = seriesMetadata.publisher,
            genre = seriesMetadata.genres.ifEmpty { null }?.joinToString(","),
            tags = seriesMetadata.tags.ifEmpty { null }?.joinToString(","),
            ageRating = seriesMetadata.ageRating
                ?.let { metadataRating ->
                    (AgeRating.values()
                        .filter { it.ageRating != null }
                        .sortedBy { it.ageRating }
                        .firstOrNull { it.ageRating == it.ageRating!!.coerceAtLeast(metadataRating) }
                        ?: AgeRating.ADULTS_ONLY_18)
                        .value
                },
            languageISO = seriesMetadata.language,
            localizedSeries = seriesMetadata.titles.find { it.type != null }?.name
        )
    }

    private fun <T> getIfNotLockedOrEmpty(patched: T?, lock: Boolean): T? =
        if (patched is Collection<*> && patched.isEmpty()) null
        else if (patched != null && !lock) patched
        else null
}
