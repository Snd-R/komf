package snd.komf.providers.comicvine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable()
class ComicVineSearchResult<T>(
    val error: String,
    val limit: Int,
    val offset: Int,
    @SerialName("number_of_page_results")
    val numberOfPageResults: Int,
    @SerialName("number_of_total_results")
    val numberOfTotalResults: Int,
    @SerialName("status_code")
    val statusCode: Int,
    val results: T,
    val version: String,
)

