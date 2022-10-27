package org.snd.mediaserver.model

import org.snd.metadata.model.ReadingDirection
import org.snd.metadata.model.SeriesStatus

data class MediaServerSeriesMetadata(
    val status: SeriesStatus,
    val title: String,
    val titleSort: String,
    val summary: String,
    val readingDirection: ReadingDirection?,
    val publisher: String?,
    val alternativePublishers: Collection<String>,
    val ageRating: Int?,
    val language: String?,
    val genres: Collection<String>,
    val tags: Collection<String>,
    val totalBookCount: Int?,
    val authors: Collection<MediaServerAuthor>,

    val statusLock: Boolean,
    val titleLock: Boolean,
    val titleSortLock: Boolean,
    val summaryLock: Boolean,
    val readingDirectionLock: Boolean,
    val publisherLock: Boolean,
    val ageRatingLock: Boolean,
    val languageLock: Boolean,
    val genresLock: Boolean,
    val tagsLock: Boolean,
    val totalBookCountLock: Boolean,
    val authorsLock: Boolean,
)
