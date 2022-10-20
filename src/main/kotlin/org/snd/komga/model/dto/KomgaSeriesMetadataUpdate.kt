package org.snd.komga.model.dto

import com.squareup.moshi.JsonClass
import org.snd.metadata.model.SeriesMetadata

@JsonClass(generateAdapter = true)
data class KomgaSeriesMetadataUpdate(
    val status: String? = null,
    val title: String? = null,
    val titleSort: String? = null,
    val summary: String? = null,
    val publisher: String? = null,
    val readingDirection: String? = null,
    val ageRating: Int? = null,
    val language: String? = null,
    val genres: Collection<String>? = null,
    val tags: Collection<String>? = null,
    val totalBookCount: Int? = null,

    val statusLock: Boolean? = null,
    val titleLock: Boolean? = null,
    val titleSortLock: Boolean? = null,
    val summaryLock: Boolean? = null,
    val publisherLock: Boolean? = null,
    val readingDirectionLock: Boolean? = null,
    val ageRatingLock: Boolean? = null,
    val languageLock: Boolean? = null,
    val genresLock: Boolean? = null,
    val tagsLock: Boolean? = null,
    val totalBookCountLock: Boolean? = null,
)

fun KomgaSeries.toMetadataResetRequest() = KomgaSeriesMetadataUpdate(
    status = SeriesMetadata.Status.ONGOING.name,
    title = name,
    titleSort = name,
    summary = "",
    publisher = "",
    readingDirection = null,
    ageRating = null,
    language = "",
    genres = emptyList(),
    tags = emptyList(),
    totalBookCount = null,
    statusLock = false,
    titleLock = false,
    titleSortLock = false,
    summaryLock = false,
    publisherLock = false,
    readingDirectionLock = false,
    ageRatingLock = false,
    languageLock = false,
    genresLock = false,
    tagsLock = false,
    totalBookCountLock = false
)
