package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KavitaAuthenticateResponse(
    val username: String,
    val email: String?,
    val token: String,
    val apiKey: String,
)