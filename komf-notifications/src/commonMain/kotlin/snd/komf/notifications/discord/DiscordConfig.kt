package snd.komf.notifications.discord

import kotlinx.serialization.Serializable

@Serializable
data class DiscordConfig(
    val webhooks: List<String>? = null,
    val embedColor: String = "1F8B4C",
    val seriesCover: Boolean = false,
)