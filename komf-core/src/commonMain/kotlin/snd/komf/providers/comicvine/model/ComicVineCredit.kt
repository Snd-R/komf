package snd.komf.providers.comicvine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComicVineCredit(
    val id: Int,
    val name: String,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String,
)

