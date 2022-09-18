package org.snd.metadata.model

data class SeriesMatchResult(
    val status: SeriesMatchStatus,
    val result: ProviderSeriesMetadata?
)
