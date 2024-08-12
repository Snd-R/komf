package snd.komf.providers.yenpress.model

import kotlinx.datetime.LocalDate
import snd.komf.model.BookRange
import kotlin.jvm.JvmInline

data class YenPressBook(
    val id: YenPressBookId,
    val name: String,
    val number: BookRange?,
    val seriesId: YenPressSeriesId,

    val authors: List<YenPressAuthor>,
    val description: String?,
    val genres: Collection<String>,
    val seriesName: String?,
    val pageCount: Int?,
    val releaseDate: LocalDate?,
    val isbn: String?,
    val ageRating: String?,
    val imprint: String?,
    val imageUrl: String?,
)

@JvmInline
value class YenPressBookId(val value: String)

@JvmInline
value class YenPressSeriesId(val value: String)

data class YenPressAuthor(val role: String, val name: String)

data class YenPressBookShort(
    val id: YenPressBookId,
    val number: BookRange?,
    val name: String?,
)

data class YenPressMoreBooksResponse(
    val nextOrd: Int?,
    val books: List<YenPressBookShort>,
)
