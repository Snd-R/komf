package snd.komf.providers.mangabaka.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaBakaSearchResponse(
    val status: Int,
    val results: List<MangaBakaSeries>
)
