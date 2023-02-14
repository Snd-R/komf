package org.snd.mediaserver.model.mediaserver

import org.snd.metadata.model.metadata.ReadingDirection
import org.snd.metadata.model.metadata.SeriesStatus
import org.snd.metadata.model.metadata.WebLink

data class MediaServerSeriesMetadata(
    val status: SeriesStatus,
    val title: String,
    val titleSort: String,
    val alternativeTitles: Collection<MediaServerAlternativeTitle>,
    val summary: String,
    val readingDirection: ReadingDirection?,
    val publisher: String?,
    val alternativePublishers: Set<String>,
    val ageRating: Int?,
    val language: String?,
    val genres: Collection<String>,
    val tags: Collection<String>,
    val totalBookCount: Int?,
    val authors: Collection<MediaServerAuthor>,
    val releaseYear: Int?,
    val links: Collection<WebLink>,

    val statusLock: Boolean,
    val titleLock: Boolean,
    val titleSortLock: Boolean,
    val alternativeTitlesLock: Boolean,
    val summaryLock: Boolean,
    val readingDirectionLock: Boolean,
    val publisherLock: Boolean,
    val ageRatingLock: Boolean,
    val languageLock: Boolean,
    val genresLock: Boolean,
    val tagsLock: Boolean,
    val totalBookCountLock: Boolean,
    val authorsLock: Boolean,
    val releaseYearLock: Boolean,
    val linksLock: Boolean,
)
