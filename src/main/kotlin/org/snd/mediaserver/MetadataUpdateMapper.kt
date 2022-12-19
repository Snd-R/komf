package org.snd.mediaserver

import org.snd.config.MetadataUpdateConfig
import org.snd.mediaserver.model.*
import org.snd.metadata.BookFilenameParser
import org.snd.metadata.comicinfo.model.AgeRating
import org.snd.metadata.comicinfo.model.ComicInfo
import org.snd.metadata.model.AuthorRole.*
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesTitle
import kotlin.math.floor

class MetadataUpdateMapper(
    private val metadataUpdateConfig: MetadataUpdateConfig,
) {

    fun toBookMetadataUpdate(
        bookMetadata: BookMetadata?,
        seriesMetadata: SeriesMetadata?,
        book: MediaServerBook
    ): MediaServerBookMetadataUpdate {
        val number = if (metadataUpdateConfig.orderBooks) {
            BookFilenameParser.getVolumes(book.name)?.first
                ?: BookFilenameParser.getChapters(book.name)?.start?.let {
                    if (floor(it) == it) it.toInt()
                    else it
                }
        } else null

        return with(book.metadata) {
            val authors = (bookMetadata?.authors?.ifEmpty { seriesMetadata?.authors } ?: seriesMetadata?.authors?.ifEmpty { null })
                ?.map { author -> MediaServerAuthor(author.name, author.role.name) }

            MediaServerBookMetadataUpdate(
                summary = getIfNotLockedOrEmpty(bookMetadata?.summary, summaryLock),
                releaseDate = getIfNotLockedOrEmpty(bookMetadata?.releaseDate, releaseDateLock),
                authors = getIfNotLockedOrEmpty(authors, authorsLock),
                tags = getIfNotLockedOrEmpty(bookMetadata?.tags, tagsLock),
                isbn = getIfNotLockedOrEmpty(bookMetadata?.isbn, isbnLock),
                number = getIfNotLockedOrEmpty(number?.toString(), numberLock),
                numberSort = getIfNotLockedOrEmpty(number?.toDouble(), numberSortLock),
            )
        }
    }

    fun toSeriesMetadataUpdate(patch: SeriesMetadata, metadata: MediaServerSeriesMetadata): MediaServerSeriesMetadataUpdate =
        with(metadata) {
            val newReadingDirection = metadataUpdateConfig.readingDirectionValue
                ?: getIfNotLockedOrEmpty(patch.readingDirection, readingDirectionLock)

            val authors = (patch.authors.map { MediaServerAuthor(it.name, it.role.name) }.ifEmpty { null })
            val seriesTitle = seriesTitle(patch.titles)

            MediaServerSeriesMetadataUpdate(
                status = getIfNotLockedOrEmpty(patch.status, statusLock),
                title =
                if (metadataUpdateConfig.seriesTitle) getIfNotLockedOrEmpty(seriesTitle, titleLock)
                else null,
                titleSort =
                if (metadataUpdateConfig.seriesTitle) getIfNotLockedOrEmpty(seriesTitle, titleSortLock)
                else null,
                summary = getIfNotLockedOrEmpty(patch.summary, summaryLock),
                publisher = getIfNotLockedOrEmpty(patch.publisher, publisherLock),
                alternativePublishers = getIfNotLockedOrEmpty(patch.alternativePublishers, publisherLock),
                readingDirection = newReadingDirection,
                ageRating = getIfNotLockedOrEmpty(patch.ageRating, ageRatingLock),
                language = getIfNotLockedOrEmpty(patch.language ?: metadataUpdateConfig.languageValue, languageLock),
                genres = getIfNotLockedOrEmpty(patch.genres, genresLock),
                tags = getIfNotLockedOrEmpty(patch.tags, tagsLock),
                totalBookCount = getIfNotLockedOrEmpty(patch.totalBookCount, totalBookCountLock),
                authors = getIfNotLockedOrEmpty(authors, authorsLock),
            )
        }

    fun toComicInfo(bookMetadata: BookMetadata?, seriesMetadata: SeriesMetadata?): ComicInfo? {
        if (bookMetadata == null && seriesMetadata == null) return null
        val authors = (bookMetadata?.authors?.ifEmpty { seriesMetadata?.authors }) ?: seriesMetadata?.authors

        return ComicInfo(
//            title = bookMetadata?.title, // disabled until common naming convention is implemented
            series = if (metadataUpdateConfig.seriesTitle) seriesMetadata?.titles?.let { seriesTitle(it) } else null,
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
            languageISO = seriesMetadata?.language ?: metadataUpdateConfig.languageValue
        )
    }

    fun toSeriesComicInfo(seriesMetadata: SeriesMetadata): ComicInfo {
        val authors = seriesMetadata.authors
        return ComicInfo(
            series = if (metadataUpdateConfig.seriesTitle) seriesTitle(seriesMetadata.titles) else null,
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
            languageISO = seriesMetadata.language ?: metadataUpdateConfig.languageValue
        )
    }

    private fun <T> getIfNotLockedOrEmpty(patched: T?, lock: Boolean): T? =
        if (patched is Collection<*> && patched.isEmpty()) null
        else if (patched != null && !lock) patched
        else null

    private fun seriesTitle(titles: Collection<SeriesTitle>): String? {
        val preferredType = metadataUpdateConfig.titleType
        val knownTitles = titles.filter { it.type != null }

        return (knownTitles.find { it.type == preferredType } ?: knownTitles.firstOrNull())?.name
    }
}
