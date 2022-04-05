package org.snd.komga.model.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KomgaWebLink(
    val label: String,
    val url: String,
)
