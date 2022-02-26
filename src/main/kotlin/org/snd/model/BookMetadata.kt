package org.snd.model

import java.time.LocalDate


data class BookMetadata(
    val title: String? =null,
    val summary: String? = null,
    val number: String? = null,
    val numberSort: Float? = null,
    val releaseDate: LocalDate? = null,
    val authors: List<Author>? = null,
    val tags: Set<String>? = null,
    val isbn: String? = null,
    val links: List<WebLink>? = null,
)
