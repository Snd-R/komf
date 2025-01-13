package snd.komf.api.mediaserver

import kotlinx.serialization.Serializable

@Serializable
data class KomfMediaServerConnectionResponse(
    val success: Boolean,
    val httpStatusCode: Int?,
    val errorMessage: String?
)