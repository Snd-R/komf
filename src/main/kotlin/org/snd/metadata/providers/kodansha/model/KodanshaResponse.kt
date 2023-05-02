package org.snd.metadata.providers.kodansha.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KodanshaResponse<T>(
    val response: T,
    val status: KodanshaResponseStatus
)

@JsonClass(generateAdapter = true)
data class KodanshaResponseStatus(
    val type: String
)