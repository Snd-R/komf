package snd.komf.notifications.discord.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Webhook(
    val type: Int,
    val id: String,
    val name: String,
    val avatar: String?,
    @SerialName("channel_id")
    val channelId: String,
    @SerialName("guild_id")
    val guildId: String,
    @SerialName("application_id")
    val applicationId: String?,
    val token: String,
)
