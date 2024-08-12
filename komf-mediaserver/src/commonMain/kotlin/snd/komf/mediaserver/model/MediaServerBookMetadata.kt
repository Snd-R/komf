package snd.komf.mediaserver.model

import kotlinx.datetime.LocalDate
import snd.komf.model.WebLink

data class MediaServerBookMetadata(
    val title: String,
    val summary: String?,
    val number: String,
    val numberSort: String?,
    val releaseDate: LocalDate?,
    val authors: List<MediaServerAuthor>,
    val tags: Collection<String>,
    val isbn: String?,
    val links: Collection<WebLink>,

    val titleLock: Boolean,
    val summaryLock: Boolean,
    val numberLock: Boolean,
    val numberSortLock: Boolean,
    val releaseDateLock: Boolean,
    val authorsLock: Boolean,
    val tagsLock: Boolean,
    val isbnLock: Boolean,
    val linksLock: Boolean,
)
