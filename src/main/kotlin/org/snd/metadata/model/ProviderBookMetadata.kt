package org.snd.metadata.model

import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class ProviderBookMetadata(
    val id: ProviderBookId? = null,
    val seriesId: ProviderSeriesId? = null,
    val metadata: BookMetadata,
)
