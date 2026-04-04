package snd.komf.providers.lambiek.model

data class LambiekSeries(
    val id: LambiekSeriesId,
    val title: String,
    val publisher: String?,
    val language: String?,
    val genres: List<String>,
    val books: List<LambiekSeriesBook>,
)

data class LambiekSeriesBook(
    val id: LambiekBookId,
    val slug: String,
    val volume: String?,
    val title: String,
    val imageUrl: String?,
)
