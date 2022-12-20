package org.snd.metadata.providers.yenpress.model

import org.snd.metadata.model.BookRange
import java.time.LocalDate

data class YenPressBook(
    val id: YenPressBookId,
    val name: String,
    val number: BookRange?,
    val publisher: String = "Yen Press",
    val releaseDate: LocalDate?,
    val description: String?,
    val imageUrl: String?,
    val genres: Collection<String>,
    val isbn: String?,
    val seriesBooks: Collection<YenPressSeriesBook>
)

data class YenPressSeriesBook(
    val id: YenPressBookId,
    val number: BookRange?,
    val name: String?
)
