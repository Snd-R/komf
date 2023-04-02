package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Info (
    val key: String,
    val value: Any?
)