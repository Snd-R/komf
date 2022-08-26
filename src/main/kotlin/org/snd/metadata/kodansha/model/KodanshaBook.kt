package org.snd.metadata.kodansha.model

import java.time.LocalDate

data class KodanshaBook(
    val id: KodanshaBookId,
    val name: String,
    val number: Int?,
    val summary: String?,
    val coverUrl: String?,
    val tags: Collection<String>,
    val authors: Collection<String>,
    val ageRating: Int?,
    val printReleaseDate: LocalDate?,
    val isbn: String?,
    val ebookReleaseDate: LocalDate?,
    val eisbn: String?,
    val pages: Int?
)
