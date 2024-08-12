package snd.komf.app.api.deprecated.dto

import kotlinx.serialization.Serializable

@Serializable
data class IdentifySeriesRequest(
    val libraryId: String? = null,
    val seriesId: String,
    val provider: String,
    val providerSeriesId: String,
    val edition: String? = null
)
