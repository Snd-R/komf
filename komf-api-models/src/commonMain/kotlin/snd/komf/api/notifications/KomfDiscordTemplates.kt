package snd.komf.api.notifications

import kotlinx.serialization.Serializable

@Serializable
data class KomfDiscordTemplates(
    val titleTemplate: String? = null,
    val titleUrlTemplate: String? = null,
    val descriptionTemplate: String? = null,
    val fields: List<EmbedFieldTemplate> = emptyList(),
    val footerTemplate: String? = null,
)

@Serializable
data class EmbedFieldTemplate(
    val nameTemplate: String,
    val valueTemplate: String,
    val inline: Boolean,
)
