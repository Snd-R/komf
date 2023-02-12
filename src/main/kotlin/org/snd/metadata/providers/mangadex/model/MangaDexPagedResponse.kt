package org.snd.metadata.providers.mangadex.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaDexPagedResponse<T>(
    val result: String,
    val response: String,
    val data: T,
    val limit: Int,
    val offset: Int,
    val total: Int,
)