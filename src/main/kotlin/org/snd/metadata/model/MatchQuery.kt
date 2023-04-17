package org.snd.metadata.model

import org.snd.metadata.model.metadata.BookRange

data class MatchQuery(
    val seriesName: String,
    val startYear: Int?,
    val bookQualifier: BookQualifier?,
)

data class BookQualifier(
    val name: String,
    val number: BookRange,
    val cover: Image?
)