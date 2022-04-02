package org.snd.komga.model.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KomgaSeries(
    val id: String,
    val name: String,
    val url: String,
    val booksCount: Int,
    val booksReadCount: Int,
    val booksUnreadCount: Int,
    val booksInProgressCount: Int,
    val metadata: KomgaSeriesMetadata,
    val deleted: Boolean,
) {
    fun seriesId(): KomgaSeriesId = KomgaSeriesId(id)
}

@JsonClass(generateAdapter = true)
data class KomgaSeriesMetadata(
    val status: String,
    val statusLock: Boolean,
    val title: String,
    val titleLock: Boolean,
    val titleSort: String,
    val titleSortLock: Boolean,
    val summary: String,
    val summaryLock: Boolean,
    val readingDirection: String,
    val readingDirectionLock: Boolean,
    val publisher: String,
    val publisherLock: Boolean,
    val ageRating: Int?,
    val ageRatingLock: Boolean,
    val language: String,
    val languageLock: Boolean,
    val genres: Set<String>,
    val genresLock: Boolean,
    val tags: Set<String>,
    val tagsLock: Boolean,
    val totalBookCount: Int?,
    val totalBookCountLock: Boolean,
)


