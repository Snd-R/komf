package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Images (

    val large: String,

    val common: String?,

    val medium: String,

    val small: String,

    val grid: String

)