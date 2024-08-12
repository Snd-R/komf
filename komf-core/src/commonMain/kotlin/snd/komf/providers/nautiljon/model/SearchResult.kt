package snd.komf.providers.nautiljon.model

data class SearchResult(
    val id: NautiljonSeriesId,
    val title: String,
    val alternativeTitle: String?,
    val description: String?,
    val imageUrl: String?,
    val type: String?,
    val volumesNumber: Int?,
    val startYear: Int?,
    val score: Double?,
)

