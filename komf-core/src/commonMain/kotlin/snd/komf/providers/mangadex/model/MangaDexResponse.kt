package snd.komf.providers.mangadex.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaDexResponse<T>(
    val result: String,
    val response: String,
    val data: T
)
@Serializable
data class MangaDexPagedResponse<T>(
    val result: String,
    val response: String,
    val data: T,
    val limit: Int,
    val offset: Int,
    val total: Int,
)
