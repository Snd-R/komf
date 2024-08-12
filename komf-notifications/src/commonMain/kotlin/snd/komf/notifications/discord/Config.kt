package snd.komf.notifications.discord

import kotlinx.serialization.Serializable

@Serializable
data class DiscordConfig(
    val title: String? = null,
    val titleUrl: String? = null,
    val descriptionTemplate: String? = "discordWebhook.vm",
    val fieldTemplates: List<DiscordFieldTemplateConfig> = emptyList(),
    val footerTemplate: String? = null,
    val seriesCover: Boolean = false,
    val colorCode: String = "1F8B4C",
    val webhooks: List<String>? = null,
    val templatesDirectory: String = "./",
)

@Serializable
data class DiscordFieldTemplateConfig(
    val name: String,
    val templateName: String,
    val inline: Boolean = false
)
