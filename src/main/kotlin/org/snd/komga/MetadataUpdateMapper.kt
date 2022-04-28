package org.snd.komga

import org.snd.config.MetadataUpdateConfig
import org.snd.komga.model.dto.KomgaAuthor
import org.snd.komga.model.dto.KomgaBookMetadata
import org.snd.komga.model.dto.KomgaBookMetadataUpdate
import org.snd.komga.model.dto.KomgaSeriesMetadata
import org.snd.komga.model.dto.KomgaSeriesMetadataUpdate
import org.snd.komga.model.dto.KomgaWebLink
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.VolumeMetadata

class MetadataUpdateMapper(
    private val metadataUpdateConfig: MetadataUpdateConfig,
) {

    fun toBookMetadataUpdate(patch: SeriesMetadata, metadata: KomgaBookMetadata): KomgaBookMetadataUpdate =
        with(metadata) {
            val authors = patch.authors?.map { author -> KomgaAuthor(author.name, author.role) }
            KomgaBookMetadataUpdate(
                authors = getIfNotLocked(authors, authorsLock)
            )
        }

    fun toBookMetadataUpdate(patch: VolumeMetadata, metadata: KomgaBookMetadata): KomgaBookMetadataUpdate =
        with(metadata) {
            KomgaBookMetadataUpdate(
                title = getIfNotLocked(patch.title, titleLock),
                summary = getIfNotLocked(patch.summary, summaryLock),
                releaseDate = getIfNotLocked(patch.releaseDate, releaseDateLock),
                authors = getIfNotLocked(patch.authors?.map { author -> KomgaAuthor(author.name, author.role) }, authorsLock),
                tags = getIfNotLocked(patch.tags, tagsLock),
                isbn = getIfNotLocked(patch.isbn, isbnLock),
                links = getIfNotLocked(patch.links?.map { KomgaWebLink(it.label, it.url) }, linksLock)
            )
        }

    fun toSeriesMetadataUpdate(patch: SeriesMetadata, metadata: KomgaSeriesMetadata): KomgaSeriesMetadataUpdate =
        with(metadata) {
            val readingDirection = metadataUpdateConfig.readingDirectionValue ?: patch.readingDirection
            KomgaSeriesMetadataUpdate(
                status = getIfNotLocked(patch.status?.toString(), statusLock),
                title = if (metadataUpdateConfig.seriesTitle) getIfNotLocked(patch.title, titleLock) else null,
                titleSort = if (metadataUpdateConfig.seriesTitle) getIfNotLocked(patch.titleSort, titleSortLock) else null,
                summary = getIfNotLocked(patch.summary, summaryLock),
                publisher = getIfNotLocked(patch.publisher, publisherLock),
                readingDirection = getIfNotLocked(readingDirection?.toString(), readingDirectionLock),
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
