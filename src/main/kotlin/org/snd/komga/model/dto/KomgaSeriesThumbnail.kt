package org.snd.komga.model.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KomgaSeriesThumbnail(
    val id: String,
    val seriesId: String,
    val type: String,
    val selected: Boolean,
)

