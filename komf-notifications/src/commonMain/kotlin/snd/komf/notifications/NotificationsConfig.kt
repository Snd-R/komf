package snd.komf.notifications

import kotlinx.serialization.Serializable
import snd.komf.notifications.apprise.AppriseConfig
import snd.komf.notifications.discord.DiscordConfig

@Serializable
data class NotificationsConfig(
    val apprise: AppriseConfig = AppriseConfig(),
    val discord: DiscordConfig = DiscordConfig(),
    val templatesDirectory: String = "./",
)
