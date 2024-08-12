package snd.komf.providers.kodansha.model

import kotlinx.serialization.Serializable

@Serializable
data class KodanshaResponse<T>(
    val response: T,
    val status: KodanshaResponseStatus
)

@Serializable
data class KodanshaResponseStatus(
    val type: String
)