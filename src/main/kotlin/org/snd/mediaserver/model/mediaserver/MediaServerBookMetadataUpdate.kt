package org.snd.mediaserver.model.mediaserver

import org.snd.metadata.model.metadata.WebLink
import java.time.LocalDate


data class MediaServerBookMetadataUpdate(
    val title: String? = null,
    val summary: String? = null,
    val number: String? = null,
    val numberSort: Double? = null,
    val releaseDate: LocalDate? = null,
    val authors: List<MediaServerAuthor>? = null,
    val tags: Collection<String>? = null,
    val isbn: String? = null,
    val links: Collection<WebLink>? = null,

    val titleLock: Boolean? = null,
    val summaryLock: Boolean? = null,
    val numberLock: Boolean? = null,
    val numberSortLock: Boolean? = null,
    val releaseDateLock: Boolean? = null,
    val authorsLock: Boolean? = null,
    val tagsLock: Boolean? = null,
    val isbnLock: Boolean? = null,
    val linksLock: Boolean? = null,
)
