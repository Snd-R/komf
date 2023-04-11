package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Collection (

    val wish: Int,

    val collect: Int,

    val doing: Int,

    @Json(name = "on_hold")
    val onHold: Int,

    val dropped: Int

)
