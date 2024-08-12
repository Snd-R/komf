package snd.komf.mediaserver.metadata

import snd.komf.mediaserver.model.MediaServerAuthor
import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookMetadataUpdate
import snd.komf.mediaserver.model.MediaServerSeriesMetadata
import snd.komf.mediaserver.model.MediaServerSeriesMetadataUpdate
import snd.komf.comicinfo.model.AgeRating
import snd.komf.comicinfo.model.ComicInfo
import snd.komf.model.AuthorRole.COLORIST
import snd.komf.model.AuthorRole.COVER
import snd.komf.model.AuthorRole.EDITOR
import snd.komf.model.AuthorRole.INKER
import snd.komf.model.AuthorRole.LETTERER
import snd.komf.model.AuthorRole.PENCILLER
import snd.komf.model.AuthorRole.TRANSLATOR
import snd.komf.model.AuthorRole.WRITER
import snd.komf.model.BookMetadata
import snd.komf.model.SeriesMetadata

class MetadataMapper {

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
                tags = getIfNotLockedOrEmpty(bookMetadata?.tags?.toList(), tagsLock),
                isbn = getIfNotLockedOrEmpty(bookMetadata?.isbn, isbnLock),
                links = getIfNotLockedOrEmpty(bookMetadata?.links, linksLock),

                // ignore lock since we can't know if komf was the one to lock number
                number = bookMetadata?.number?.toString(),
                numberSort = bookMetadata?.number?.start,

                // lock if number is not null; do not unlock if was locked
                numberLock = numberLock || bookMetadata?.number != null,
                numberSortLock = numberSortLock || bookMetadata?.numberSort != null,
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
                genres = getIfNotLockedOrEmpty(patch.genres.toList(), genresLock),
                tags = getIfNotLockedOrEmpty(patch.tags.toList(), tagsLock),
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
            month = bookMetadata?.releaseDate?.monthNumber,
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
                    (AgeRating.entries
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
                    (AgeRating.entries
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
