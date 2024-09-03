package snd.komf.providers.kodansha.model

import kotlinx.serialization.Serializable

@Serializable
data class KodanshaThumbnail(
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long? = null,
    val url: String,
    val color: String? = null,
)
