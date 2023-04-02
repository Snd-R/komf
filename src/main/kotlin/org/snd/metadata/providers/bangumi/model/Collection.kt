package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Collection (

    val wish: Int,

    val collect: Int,

    val doing: Int,

    val on_hold: Int,

    val dropped: Int

)
