package snd.komf.notifications.apprise

import kotlinx.serialization.Serializable

@Serializable
data class AppriseConfig(
    val urls: List<String>? = null,
)