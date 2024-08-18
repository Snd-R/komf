package snd.komf.api.notifications

import kotlinx.serialization.Serializable

@Serializable
data class KomfRenderRequest(
    val context: KomfNotificationContext = KomfNotificationContext(),
    val templates: KomfDiscordTemplates
)
