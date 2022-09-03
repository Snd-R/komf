package org.snd.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IdentifySeriesRequest(
    val seriesId: String,
    val provider: String,
    val providerSeriesId: String,
    val edition: String?
)
