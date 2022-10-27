package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass

data class KavitaPage<T>(
    val content: Collection<T>,
    val pagination: Pagination
)

@JsonClass(generateAdapter = true)
data class Pagination(
    val currentPage: Int,
    val itemsPerPage: Int,
    val totalItems: Int,
    val totalPages: Int,
)