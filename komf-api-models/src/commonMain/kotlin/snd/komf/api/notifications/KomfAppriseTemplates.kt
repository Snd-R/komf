package snd.komf.api.notifications

import kotlinx.serialization.Serializable

@Serializable
data class KomfAppriseTemplates(
    val titleTemplate: String? = null,
    val bodyTemplate: String? = null,
)
