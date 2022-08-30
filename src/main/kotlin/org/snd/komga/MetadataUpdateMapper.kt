package org.snd.komga

import org.snd.config.MetadataUpdateConfig
import org.snd.komga.model.dto.KomgaAuthor
import org.snd.komga.model.dto.KomgaBookMetadata
import org.snd.komga.model.dto.KomgaBookMetadataUpdate
import org.snd.komga.model.dto.KomgaSeriesMetadata
import org.snd.komga.model.dto.KomgaSeriesMetadataUpdate
import org.snd.komga.model.dto.KomgaWebLink
import org.snd.metadata.comicinfo.model.AgeRating
import org.snd.metadata.comicinfo.model.ComicInfo
import org.snd.metadata.model.AuthorRole.*
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.SeriesMetadata

class MetadataUpdateMapper(
    private val metadataUpdateConfig: MetadataUpdateConfig,
) {

    fun toBookMetadataUpdate(bookMetadata: BookMetadata?, seriesMetadata: SeriesMetadata, komgaMetadata: KomgaBookMetadata): KomgaBookMetadataUpdate =
        with(komgaMetadata) {
            val authors = (bookMetadata?.authors ?: seriesMetadata.authors)?.map { author -> KomgaAuthor(author.name, author.role.name) }
            KomgaBookMetadataUpdate(
                summary = getIfNotLocked(bookMetadata?.summary, summaryLock),
                releaseDate = getIfNotLocked(bookMetadata?.releaseDate, releaseDateLock),
                authors = getIfNotLocked(authors, authorsLock),
                tags = getIfNotLocked(bookMetadata?.tags, tagsLock),
                isbn = getIfNotLocked(bookMetadata?.isbn, isbnLock),
                links = getIfNotLocked(bookMetadata?.links?.map { KomgaWebLink(it.label, it.url) }, linksLock)
            )
        }

    fun toSeriesMetadataUpdate(patch: SeriesMetadata, metadata: KomgaSeriesMetadata): KomgaSeriesMetadataUpdate =
        with(metadata) {
            KomgaSeriesMetadataUpdate(
                status = getIfNotLocked(patch.status?.toString(), statusLock),
                title = if (metadataUpdateConfig.seriesTitle) getIfNotLocked(patch.title, titleLock) else null,
                titleSort = if (metadataUpdateConfig.seriesTitle) getIfNotLocked(patch.titleSort, titleSortLock) else null,
                summary = getIfNotLocked(patch.summary, summaryLock),
                publisher = getIfNotLocked(patch.publisher, publisherLock),
                readingDirection = getIfNotLocked(patch.readingDirection?.toString(), readingDirectionLock),
                ageRating = getIfNotLocked(patch.ageRating, ageRatingLock),
                language = getIfNotLocked(patch.language, languageLock),
                genres = getIfNotLocked(patch.genres, genresLock),
                tags = getIfNotLocked(patch.tags, tagsLock),
                totalBookCount = getIfNotLocked(patch.totalBookCount, totalBookCountLock),
            )
        }

    fun toComicInfo(bookMetadata: BookMetadata?, seriesMetadata: SeriesMetadata): ComicInfo {
        return ComicInfo(
            title = bookMetadata?.title,
            series = seriesMetadata.title,
            number = bookMetadata?.number?.toString(),
            count = seriesMetadata.totalBookCount,
            summary = bookMetadata?.summary,
            year = bookMetadata?.releaseDate?.year,
            month = bookMetadata?.releaseDate?.monthValue,
            day = bookMetadata?.releaseDate?.dayOfMonth,
            writer = (bookMetadata?.authors ?: seriesMetadata.authors)
                ?.filter { it.role == WRITER }?.joinToString(",") { it.name },
            penciller = (bookMetadata?.authors ?: seriesMetadata.authors)
                ?.filter { it.role == PENCILLER }?.joinToString(",") { it.name },
            inker = (bookMetadata?.authors ?: seriesMetadata.authors)
                ?.filter { it.role == INKER }?.joinToString(",") { it.name },
            colorist = (bookMetadata?.authors ?: seriesMetadata.authors)
                ?.filter { it.role == COLORIST }?.joinToString(",") { it.name },
            letterer = (bookMetadata?.authors ?: seriesMetadata.authors)
                ?.filter { it.role == LETTERER }?.joinToString(",") { it.name },
            coverArtist = (bookMetadata?.authors ?: seriesMetadata.authors)
                ?.filter { it.role == COVER }?.joinToString(",") { it.name },
            editor = (bookMetadata?.authors ?: seriesMetadata.authors)
                ?.filter { it.role == EDITOR }?.joinToString(",") { it.name },
            translator = (bookMetadata?.authors ?: seriesMetadata.authors)
                ?.filter { it.role == TRANSLATOR }?.joinToString(",") { it.name },
            publisher = seriesMetadata.publisher,
            genre = seriesMetadata.genres?.joinToString(","),
            tags = bookMetadata?.tags?.joinToString(","),
            ageRating = seriesMetadata.ageRating
                ?.let { metadataRating ->
                    AgeRating.values().filter { it.ageRating != null }
                        .maxByOrNull { it.ageRating!!.coerceAtLeast(metadataRating) }?.name
                }
        )

    }

    private fun <T> getIfNotLocked(patched: T?, lock: Boolean): T? =
        if (patched != null && !lock) patched
        else null
}
