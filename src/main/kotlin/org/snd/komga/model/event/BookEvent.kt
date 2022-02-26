package org.snd.komga.model.event

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookEvent(
    val bookId: String,
    val seriesId: String,
    val libraryId: String,
)
