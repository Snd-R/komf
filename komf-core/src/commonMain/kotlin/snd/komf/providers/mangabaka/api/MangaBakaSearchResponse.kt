package snd.komf.providers.mangabaka.api

import kotlinx.serialization.Serializable
import snd.komf.providers.mangabaka.MangaBakaSeries

@Serializable
data class MangaBakaSearchResponse(
    val status: Int,
    val results: List<MangaBakaSeries>
)