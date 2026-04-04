package snd.komf.providers.lambiek.model

data class LambiekBook(
    val id: LambiekBookId,
    val seriesId: LambiekSeriesId?,
    val title: String?,
    val volume: String?,
    val isbn: String?,
    val releaseDate: String?,
    val illustrator: String?,
    val writer: String?,
    val publisher: String?,
    val language: String?,
    val genres: List<String>,
    val pageCount: Int?,
    val description: String?,
    val imageUrl: String?,
)
