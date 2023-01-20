package org.snd.mediaserver.komga.model.dto

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.model.MediaServerSeriesMetadataUpdate
import org.snd.metadata.model.SeriesStatus.ONGOING

@JsonClass(generateAdapter = true)
data class KomgaSeriesMetadataUpdate(
    val status: String? = null,
    val title: String? = null,
    val titleSort: String? = null,
    val alternateTitles: Collection<KomgaAlternativeTitle>? = null,
    val summary: String? = null,
    val publisher: String? = null,
    val readingDirection: String? = null,
    val ageRating: Int? = null,
    val language: String? = null,
    val genres: Collection<String>? = null,
    val tags: Collection<String>? = null,
    val totalBookCount: Int? = null,
    val links: Collection<KomgaWebLink>? = null,

    val statusLock: Boolean? = null,
    val titleLock: Boolean? = null,
    val titleSortLock: Boolean? = null,
    val alternateTitlesLock: Boolean? = null,
    val summaryLock: Boolean? = null,
    val publisherLock: Boolean? = null,
    val readingDirectionLock: Boolean? = null,
    val ageRatingLock: Boolean? = null,
    val languageLock: Boolean? = null,
    val genresLock: Boolean? = null,
    val tagsLock: Boolean? = null,
    val totalBookCountLock: Boolean? = null,
    val linksLock: Boolean? = null,
)

fun metadataResetRequest(name: String) = KomgaSeriesMetadataUpdate(
    status = ONGOING.name,
    title = name,
    titleSort = name,
    alternateTitles = null,
    summary = "",
    publisher = "",
    readingDirection = null,
    ageRating = null,
    language = "",
    genres = emptyList(),
    tags = emptyList(),
    totalBookCount = null,
    links = null,
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
    totalBookCountLock = false,
    linksLock = false,
)

fun MediaServerSeriesMetadataUpdate.metadataUpdateRequest() = KomgaSeriesMetadataUpdate(
    status = status?.name,
    title = title?.name,
    titleSort = titleSort?.name,
    alternateTitles = alternativeTitles?.mapNotNull { (name, type) ->
        type?.let { KomgaAlternativeTitle(it.label, name) }
    }?.distinctBy { it.label },
    summary = summary,
    publisher = publisher,
    readingDirection = readingDirection?.name,
    ageRating = ageRating,
    language = language,
    genres = genres,
    tags = tags,
    totalBookCount = totalBookCount,
    links = links?.map { KomgaWebLink(it.label, it.url) },

    statusLock = statusLock,
    titleLock = titleLock,
    titleSortLock = titleSortLock,
    alternateTitlesLock = alternativeTitlesLock,
    summaryLock = summaryLock,
    publisherLock = publisherLock,
    readingDirectionLock = readingDirectionLock,
    ageRatingLock = ageRatingLock,
    languageLock = languageLock,
    genresLock = genresLock,
    tagsLock = tagsLock,
    totalBookCountLock = totalBookCountLock,
    linksLock = linksLock,
)