package org.snd.metadata.yenpress.model

import java.time.LocalDate

data class YenPressBook(
    val id: YenPressBookId,
    val name: String,
    val number: Int?,
    val publisher: String = "Yen Press",
    val releaseDate: LocalDate?,
    val description: String?,
    val imageUrl: String?,
    val genres: Collection<String>,
    val isbn: String,
    val seriesBooks: Collection<YenPressSeriesBook>
)

data class YenPressSeriesBook(
    val id: YenPressBookId,
    val number: Int?,
    val name: String?
)
