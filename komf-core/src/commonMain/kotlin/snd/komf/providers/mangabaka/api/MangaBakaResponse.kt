package snd.komf.providers.mangabaka.api

import kotlinx.serialization.Serializable
import snd.komf.providers.mangabaka.MangaBakaSeries

@Serializable
data class MangaBakaResponse(
    val status: Int,
    val data: MangaBakaSeries
)