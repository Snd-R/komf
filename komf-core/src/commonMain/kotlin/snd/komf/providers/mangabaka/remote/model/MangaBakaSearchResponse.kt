package snd.komf.providers.mangabaka.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaBakaSearchResponse(
    val status: Int,
    val results: List<MangaBakaSeries>
)
