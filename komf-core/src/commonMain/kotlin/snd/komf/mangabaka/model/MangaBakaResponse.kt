package snd.komf.mangabaka.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaBakaResponse<T>(
    val status: Int,
    val pagination: MangaBakaPagination? = null,
    val data: T
)

@Serializable
data class MangaBakaPagination(
    val count: Int,
    val next: String?,
    val previous: String?,
    val limit: Int
)