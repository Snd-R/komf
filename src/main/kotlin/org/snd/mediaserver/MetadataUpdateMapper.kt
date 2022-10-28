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
            val authors = (bookMetadata?.authors?.ifEmpty { seriesMetadata?.authors } ?: seriesMetadata?.authors)
                ?.map { author -> MediaServerAuthor(author.name, author.role.name) } ?: emptyList()

            MediaServerBookMetadataUpdate(
                summary = getIfNotLockedOrEmpty(bookMetadata?.summary, summaryLock) ?: summary,
                releaseDate = getIfNotLockedOrEmpty(bookMetadata?.releaseDate, releaseDateLock) ?: releaseDate,
                authors = getIfNotLockedOrEmpty(authors, authorsLock) ?: metadata.authors,
                tags = getIfNotLockedOrEmpty(bookMetadata?.tags, tagsLock) ?: tags,
                isbn = getIfNotLockedOrEmpty(bookMetadata?.isbn, isbnLock) ?: isbn,
            )
        }
    }

    fun toSeriesMetadataUpdate(patch: SeriesMetadata, metadata: MediaServerSeriesMetadata): MediaServerSeriesMetadataUpdate =
        with(metadata) {
            val newReadingDirection = metadataUpdateConfig.readingDirectionValue
                ?: getIfNotLockedOrEmpty(patch.readingDirection, readingDirectionLock)

            val authors = (patch.authors.map { MediaServerAuthor(it.name, it.role.name) }.ifEmpty { metadata.authors })

            MediaServerSeriesMetadataUpdate(
                status = getIfNotLockedOrEmpty(patch.status, statusLock) ?: status,
                title = if (metadataUpdateConfig.seriesTitle) getIfNotLockedOrEmpty(patch.title, titleLock) ?: title else title,
                titleSort = if (metadataUpdateConfig.seriesTitle) getIfNotLockedOrEmpty(patch.titleSort, titleSortLock)
                    ?: titleSort else titleSort,
                summary = getIfNotLockedOrEmpty(patch.summary, summaryLock) ?: summary,
                publisher = getIfNotLockedOrEmpty(patch.publisher, publisherLock) ?: publisher,
                alternativePublishers = getIfNotLockedOrEmpty(patch.alternativePublishers, publisherLock) ?: alternativePublishers,
                readingDirection = newReadingDirection ?: readingDirection,
                ageRating = getIfNotLockedOrEmpty(patch.ageRating, ageRatingLock) ?: ageRating,
                language = getIfNotLockedOrEmpty(patch.language, languageLock) ?: language,
                genres = getIfNotLockedOrEmpty(patch.genres, genresLock) ?: genres,
                tags = getIfNotLockedOrEmpty(patch.tags, tagsLock) ?: tags,
                totalBookCount = getIfNotLockedOrEmpty(patch.totalBookCount, totalBookCountLock) ?: totalBookCount,
                authors = getIfNotLockedOrEmpty(authors, authorsLock) ?: metadata.authors
            )
        }

    fun toComicInfo(bookMetadata: BookMetadata?, seriesMetadata: SeriesMetadata?): ComicInfo? {
        if (bookMetadata == null && seriesMetadata == null) return null
        val authors = (bookMetadata?.authors?.ifEmpty { seriesMetadata?.authors }) ?: seriesMetadata?.authors

        return ComicInfo(
            title = bookMetadata?.title,
            series = seriesMetadata?.title,
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
                    AgeRating.values().filter { it.ageRating != null }
                        .maxByOrNull { it.ageRating!!.coerceAtLeast(metadataRating) }?.value
                }
        )
    }

    fun toMylarMetadata(series: MediaServerSeries, seriesMetadata: SeriesMetadata): MylarMetadata {
        val status = when (seriesMetadata.status) {
            SeriesStatus.ENDED -> MylarStatus.Ended
            SeriesStatus.ONGOING -> MylarStatus.Continuing
            else -> MylarStatus.Ended
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
            ageRating = seriesMetadata.ageRating
                ?.let { metadataRating ->
                    MylarAgeRating.values().filter { it.ageRating != null }
                        .maxByOrNull { it.ageRating!!.coerceAtLeast(metadataRating) }?.value
                },
            comicImage = "",
            totalIssues = seriesMetadata.totalBookCount ?: 0,
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
