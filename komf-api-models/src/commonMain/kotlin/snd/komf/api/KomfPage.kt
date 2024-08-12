package snd.komf.api

import kotlinx.serialization.Serializable

@Serializable
data class KomfPage<T>(
    val content: T,
    val totalPages: Int,
    val currentPage: Int,
)