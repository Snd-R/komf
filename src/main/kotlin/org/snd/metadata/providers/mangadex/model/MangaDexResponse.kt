package org.snd.metadata.providers.mangadex.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaDexResponse<T>(
    val result: String,
    val response: String,
    val data: T
)