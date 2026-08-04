package snd.komf.providers.ehentai.model

import kotlinx.serialization.Serializable

@Serializable
data class EHentaiResponse(
    val gmetadata: List<EHentaiBook> = emptyList(),
    val gid: Int? = null,
    val error: String? = null
)