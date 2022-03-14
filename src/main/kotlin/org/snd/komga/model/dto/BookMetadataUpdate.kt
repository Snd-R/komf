package org.snd.komga.model.dto

import com.squareup.moshi.JsonClass
import java.time.LocalDate


@JsonClass(generateAdapter = true)
data class BookMetadataUpdate(
    val title: String? = null,
    val summary: String? = null,
    val number: String? = null,
    val numberSort: Float? = null,
    val releaseDate: LocalDate? = null,
    val authors: List<AuthorUpdate>? = null,
    val tags: Set<String>? = null,
    val isbn: String? = null,
    val links: List<WebLinkUpdate>? = null,

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

@JsonClass(generateAdapter = true)
data class AuthorUpdate(
    val name: String? = null,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class WebLinkUpdate(
    val label: String? = null,
    val url: String? = null
)
