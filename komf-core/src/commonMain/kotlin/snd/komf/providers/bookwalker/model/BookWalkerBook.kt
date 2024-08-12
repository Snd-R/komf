package snd.komf.providers.bookwalker.model

import kotlinx.datetime.LocalDate
import snd.komf.model.BookRange
import kotlin.jvm.JvmInline

@JvmInline
value class BookWalkerBookId(val id: String)

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

