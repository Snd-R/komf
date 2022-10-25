package org.snd.mediaserver.model

data class MediaServerSeriesMetadata(
    val status: String,
    val title: String,
    val titleSort: String,
    val summary: String,
    val readingDirection: String?,
    val publisher: String,
    val ageRating: Int?,
    val language: String,
    val genres: Collection<String>,
    val tags: Collection<String>,
    val totalBookCount: Int?,

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
)
