package org.snd.noifications.imgur.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImgurCredits(
    @Json(name = "UserLimit")
    val userLimit: Int,
    @Json(name = "UserRemaining")
    val userRemaining: Int,
    @Json(name = "UserReset")
    val userReset: Long,
    @Json(name = "ClientLimit")
    val clientLimit: Int,
    @Json(name = "ClientRemaining")
    val clientRemaining: Int,
)
