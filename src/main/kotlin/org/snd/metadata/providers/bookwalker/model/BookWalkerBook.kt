package org.snd.metadata.providers.bookwalker.model

import org.snd.metadata.model.metadata.BookRange
import java.time.LocalDate

data class BookWalkerBook(
    val id: BookWalkerBookId,
    val seriesId: BookWalkerSeriesId?,
    val name: String,
    val number: BookRange?,

    val seriesTitle: String?,
    val japaneseTitle: String?,
    val romajiTitle: String?,
    val artists: Collection<String>,
    val authors: Collection<String>,
    val publisher: String,
    val genres: Collection<String>,
    val availableSince: LocalDate?,

    val synopsis: String?,
    val imageUrl: String?,
)

