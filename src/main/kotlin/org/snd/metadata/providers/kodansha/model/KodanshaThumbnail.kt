package org.snd.metadata.providers.kodansha.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KodanshaThumbnail(
    val width: Int?,
    val height: Int?,
    val fileSize: Long?,
    val url: String,
    val color: String?,
)

