package snd.komf.api.notifications

import kotlinx.serialization.Serializable

@Serializable
data class KomfAppriseRenderResult(
    val title: String?,
    val body: String,
)

