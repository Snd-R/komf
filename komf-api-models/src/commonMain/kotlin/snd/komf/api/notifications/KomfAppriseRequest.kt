package snd.komf.api.notifications

import kotlinx.serialization.Serializable

@Serializable
data class KomfAppriseRequest(
    val context: KomfNotificationContext = KomfNotificationContext(),
    val templates: KomfAppriseTemplates
)
