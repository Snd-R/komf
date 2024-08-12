package snd.komf.mediaserver.model

import snd.komf.model.ReadingDirection
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.WebLink

data class MediaServerSeriesMetadataUpdate(
    val status: SeriesStatus? = null,
    val title: SeriesTitle? = null,
    val alternativeTitles: Collection<SeriesTitle>? = null,
    val titleSort: SeriesTitle? = null,
    val summary: String? = null,
    val readingDirection: ReadingDirection? = null,
    val publisher: String? = null,
    val alternativePublishers: Collection<String>? = null,
    val ageRating: Int? = null,
    val language: String? = null,
    val genres: List<String>? = null,
    val tags: List<String>? = null,
    val totalBookCount: Int? = null,
    val authors: Collection<MediaServerAuthor>? = null,
    val releaseYear: Int? = null,
    val links: Collection<WebLink>? = null,

    val statusLock: Boolean? = null,
    val titleLock: Boolean? = null,
    val titleSortLock: Boolean? = null,
    val alternativeTitlesLock: Boolean? = null,
    val summaryLock: Boolean? = null,
    val readingDirectionLock: Boolean? = null,
    val publisherLock: Boolean? = null,
    val ageRatingLock: Boolean? = null,
    val languageLock: Boolean? = null,
    val genresLock: Boolean? = null,
    val tagsLock: Boolean? = null,
    val totalBookCountLock: Boolean? = null,
    val authorsLock: Boolean? = null,
    val releaseYearLock: Boolean? = null,
    val linksLock: Boolean? = null
)
