package org.snd.metadata.providers.bookwalker.model

import java.time.LocalDate

data class BookWalkerBook(
    val id: BookWalkerBookId,
    val name: String,
    val number: Int?,

    val seriesTitle: String,
    val japaneseTitle: String?,
    val artists: Collection<String>,
    val authors: Collection<String>,
    val publisher: String,
    val genres: Collection<String>,
    val availableSince: LocalDate?,

    val synopsis: String?,
    val imageUrl: String?,
)

