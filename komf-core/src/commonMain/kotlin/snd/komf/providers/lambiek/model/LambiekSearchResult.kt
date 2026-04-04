package snd.komf.providers.lambiek.model

data class LambiekSearchResult(
    val id: LambiekSeriesId,
    val seriesName: String,
    val artistName: String?,
)
