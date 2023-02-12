package org.snd.metadata.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebLink(
    val label: String,
    val url: String,
)
