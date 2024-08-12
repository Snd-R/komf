package snd.komf.providers.mangaupdates.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import snd.komf.providers.mangaupdates.model.SearchResult

@Serializable
data class SearchResultPage(
    @SerialName("total_hits")
    val totalHits: Int,
    val page: Int,
    @SerialName("per_page")
    val perPage: Int,
    val results: Collection<SearchResultHit>
)

@Serializable
data class SearchResultHit(
    val record: SearchResult,
    @SerialName("hit_title")
    val hitTitle: String?,
)
