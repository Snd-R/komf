package snd.komf.providers.webtoons.model

data class SearchResult(
    val id: WebtoonsSeriesId,
    val title: String,
    val url: String,
    val imageUrl: String?,
    val genre: String,
    val author: String,
    val views: String,
)