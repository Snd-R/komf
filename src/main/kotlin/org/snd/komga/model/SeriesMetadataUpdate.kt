package org.snd.komga.model

import com.squareup.moshi.JsonClass
import org.snd.model.SeriesMetadata

@JsonClass(generateAdapter = true)
data class SeriesMetadataUpdate(
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

fun SeriesMetadata.toSeriesUpdate() = SeriesMetadataUpdate(
    status = status?.toString(),
    title = title,
    titleSort = titleSort,
    summary = summary,
    publisher = publisher,
    readingDirection = readingDirection?.toString(),
    ageRating = ageRating,
    language = language,
    genres = genres,
    tags = tags,
    totalBookCount = totalBookCount,
)
