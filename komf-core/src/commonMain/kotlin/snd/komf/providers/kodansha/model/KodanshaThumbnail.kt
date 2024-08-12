package snd.komf.providers.kodansha.model

import kotlinx.serialization.Serializable

@Serializable
data class KodanshaThumbnail(
    val width: Int?,
    val height: Int?,
    val fileSize: Long?,
    val url: String,
    val color: String?,
)

