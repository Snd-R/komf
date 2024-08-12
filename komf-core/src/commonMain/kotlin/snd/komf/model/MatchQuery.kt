package snd.komf.model

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