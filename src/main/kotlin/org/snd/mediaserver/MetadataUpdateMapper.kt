package org.snd.mediaserver

import org.snd.config.MetadataUpdateConfig
import org.snd.mediaserver.model.*
import org.snd.metadata.comicinfo.model.AgeRating
import org.snd.metadata.comicinfo.model.ComicInfo
import org.snd.metadata.model.AuthorRole.*
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesStatus
import org.snd.metadata.mylar.model.MylarAgeRating
import org.snd.metadata.mylar.model.MylarMetadata
import org.snd.metadata.mylar.model.MylarStatus

class MetadataUpdateMapper(
    private val metadataUpdateConfig: MetadataUpdateConfig,
) {

    fun toBookMetadataUpdate(
        bookMetadata: BookMetadata?,
        seriesMetadata: SeriesMetadata?,
        metadata: MediaServerBookMetadata
    ): MediaServerBookMetadataUpdate? {
        if (bookMetadata == null && seriesMetadata == null) return null

        return with(metadata) {
            val authors = (bookMetadata?.authors?.ifEmpty { seriesMetadata?.authors }
                ?: seriesMetadata?.authors?.ifEmpty { null })?.map { author -> MediaServerAuthor(author.name, author.role.name) }
                ?: emptyList()

            MediaServerBookMetadataUpdate(
                summary = getIfNotLockedOrEmpty(bookMetadata?.summary, summaryLock),
                releaseDate = getIfNotLockedOrEmpty(bookMetadata?.releaseDate, releaseDateLock),
                authors = getIfNotLockedOrEmpty(authors, authorsLock),
                tags = getIfNotLockedOrEmpty(bookMetadata?.tags, tagsLock),
                isbn = getIfNotLockedOrEmpty(bookMetadata?.isbn, isbnLock),
            )
        }
    }

    fun toSeriesMetadataUpdate(patch: SeriesMetadata, metadata: MediaServerSeriesMetadata): MediaServerSeriesMetadataUpdate =
        with(metadata) {
            val newReadingDirection = metadataUpdateConfig.readingDirectionValue
                ?: getIfNotLockedOrEmpty(patch.readingDirection, readingDirectionLock)

            val authors = (patch.authors.map { MediaServerAuthor(it.name, it.role.name) }.ifEmpty { null })

            MediaServerSeriesMetadataUpdate(
                status = getIfNotLockedOrEmpty(patch.status, statusLock),
                title = if (metadataUpdateConfig.seriesTitle) getIfNotLockedOrEmpty(patch.title, titleLock) else null,
                titleSort = if (metadataUpdateConfig.seriesTitle) getIfNotLockedOrEmpty(patch.titleSort, titleSortLock) else null,
                summary = getIfNotLockedOrEmpty(patch.summary, summaryLock),
                publisher = getIfNotLockedOrEmpty(patch.publisher, publisherLock),
                alternativePublishers = getIfNotLockedOrEmpty(patch.alternativePublishers, publisherLock),
                readingDirection = newReadingDirection,
                ageRating = getIfNotLockedOrEmpty(patch.ageRating, ageRatingLock),
                language = getIfNotLockedOrEmpty(patch.language, languageLock),
                genres = getIfNotLockedOrEmpty(patch.genres, genresLock),
                tags = getIfNotLockedOrEmpty(patch.tags, tagsLock),
                totalBookCount = getIfNotLockedOrEmpty(patch.totalBookCount, totalBookCountLock),
                authors = getIfNotLockedOrEmpty(authors, authorsLock)
            )
        }

    fun toComicInfo(bookMetadata: BookMetadata?, seriesMetadata: SeriesMetadata?): ComicInfo? {
        if (bookMetadata == null && seriesMetadata == null) return null
        val authors = (bookMetadata?.authors?.ifEmpty { seriesMetadata?.authors }) ?: seriesMetadata?.authors

        return ComicInfo(
//            title = bookMetadata?.title, // disabled until common naming convention is implemented
            series = if (metadataUpdateConfig.seriesTitle) seriesMetadata?.title else null,
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
                }
        )
    }

    fun toSeriesComicInfo(seriesMetadata: SeriesMetadata): ComicInfo {
        val authors = seriesMetadata.authors
        return ComicInfo(
            series = if (metadataUpdateConfig.seriesTitle) seriesMetadata.title else null,
            number = "1",
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
                }
        )
    }

    fun toMylarMetadata(series: MediaServerSeries, seriesMetadata: SeriesMetadata): MylarMetadata {
        val status = when (seriesMetadata.status) {
            SeriesStatus.ENDED -> MylarStatus.Ended
            SeriesStatus.ONGOING -> MylarStatus.Continuing
            else -> MylarStatus.Continuing
        }

        return MylarMetadata(
            type = "",
            publisher = seriesMetadata.publisher ?: "",
            imprint = null,
            name = if (metadataUpdateConfig.seriesTitle) seriesMetadata.title ?: series.name else series.name,
            comicid = "",
            cid = "",
            year = 0,
            descriptionText = null,
            descriptionFormatted = seriesMetadata.summary,
            volume = null,
            bookType = "",
            ageRating = seriesMetadata.ageRating?.let { metadataRating ->
                MylarAgeRating.values().filter { it.ageRating != null }
                    .maxByOrNull { it.ageRating!!.coerceAtLeast(metadataRating) }?.value
            },
            comicImage = "",
            totalIssues = seriesMetadata.totalBookCount ?: series.booksCount ?: 1,
            publicationRun = "",
            status = status,
            collects = emptyList(),
        )
    }

    private fun <T> getIfNotLockedOrEmpty(patched: T?, lock: Boolean): T? =
        if (patched is Collection<*> && patched.isEmpty()) null
        else if (patched != null && !lock) patched
        else null
}
