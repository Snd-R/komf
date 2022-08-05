package org.snd.komga.model.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Page<T>(
    val content: List<T>,
    val pageable: Pageable,
    val totalElements: Int,
    val totalPages: Int,
    val last: Boolean,
    val number: Int,
    val sort: Sort,
    val first: Boolean,
    val numberOfElements: Int,
    val size: Int,
    val empty: Boolean
)

@JsonClass(generateAdapter = true)
data class Pageable(
    val sort: Sort,
    val pageNumber: Int,
    val pageSize: Int,
    val offset: Int,
    val paged: Boolean,
    val unpaged: Boolean,

    )

@JsonClass(generateAdapter = true)
data class Sort(
    val sorted: Boolean,
    val unsorted: Boolean,
    val empty: Boolean
)
