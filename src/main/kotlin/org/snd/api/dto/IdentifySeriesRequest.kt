package org.snd.api.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IdentifySeriesRequest(
    val libraryId: String?,
    val seriesId: String,
    val provider: String,
    val providerSeriesId: String,
    val edition: String?
)
