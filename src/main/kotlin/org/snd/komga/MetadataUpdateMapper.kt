package org.snd.komga

import org.snd.komga.model.dto.KomgaAuthor
import org.snd.komga.model.dto.KomgaBookMetadata
import org.snd.komga.model.dto.KomgaBookMetadataUpdate
import org.snd.komga.model.dto.KomgaSeriesMetadata
import org.snd.komga.model.dto.KomgaSeriesMetadataUpdate
import org.snd.komga.model.dto.WebLink
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.VolumeMetadata

class MetadataUpdateMapper {

    fun toBookMetadataUpdate(patch: SeriesMetadata, metadata: KomgaBookMetadata): KomgaBookMetadataUpdate =
        with(metadata) {
            val authors = patch.authors?.map { author -> KomgaAuthor(author.name, author.role) }
            KomgaBookMetadataUpdate(
                authors = getIfNotLocked(authors, authorsLock)
            )
        }

    fun toBookMetadataUpdate(patch: VolumeMetadata, metadata: KomgaBookMetadata): KomgaBookMetadataUpdate =
        with(metadata) {
            val authors = patch.authors?.map { author -> KomgaAuthor(author.name, author.role) }
            KomgaBookMetadataUpdate(
                title = title,
                summary = summary,
                releaseDate = releaseDate,
                authors = authors?.map { KomgaAuthor(it.name, it.role) },
                tags = tags,
                isbn = isbn,
                links = links.map { WebLink(it.label, it.url) }
            )
        }

    fun toSeriesMetadataUpdate(patch: SeriesMetadata, metadata: KomgaSeriesMetadata): KomgaSeriesMetadataUpdate =
        with(metadata) {
            KomgaSeriesMetadataUpdate(
                status = getIfNotLocked(patch.status?.toString(), statusLock),
                title = getIfNotLocked(patch.title, titleLock),
                titleSort = getIfNotLocked(patch.titleSort, titleSortLock),
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

    private fun <T> getIfNotLocked(patched: T?, lock: Boolean): T? =
        if (patched != null && !lock) patched
        else null
}
