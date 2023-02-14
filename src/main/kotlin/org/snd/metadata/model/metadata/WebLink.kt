package org.snd.metadata.model.metadata

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebLink(
    val label: String,
    val url: String,
)
