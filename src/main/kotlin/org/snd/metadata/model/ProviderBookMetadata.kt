package org.snd.metadata.model


data class ProviderBookMetadata(
    val id: ProviderBookId? = null,
    val seriesId: ProviderSeriesId? = null,
    val metadata: BookMetadata,
)
