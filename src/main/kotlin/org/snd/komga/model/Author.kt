package org.snd.komga.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Author(
    val name: String,
    val role: String,
)

