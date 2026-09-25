package snd.komf.mangabaka.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaBakaSearchResponse(
    val status: Int,
    val data: List<MangaBakaSeries>
)