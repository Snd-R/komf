package snd.komf.api.notifications

import kotlinx.serialization.Serializable

@Serializable
data class KomfTemplateRenderResult(
    val title: String?,
    val titleUrl: String?,
    val description: String?,
    val fields: List<EmbedField>,
    val footer: String?,
)

@Serializable
data class EmbedField(
    val name: String,
    val value: String,
    val inline: Boolean,
)
