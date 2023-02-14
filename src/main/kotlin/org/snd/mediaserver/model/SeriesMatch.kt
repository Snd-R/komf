package org.snd.mediaserver.model

import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId
import org.snd.metadata.model.Provider
import org.snd.metadata.model.metadata.ProviderSeriesId

data class SeriesMatch(
    val seriesId: MediaServerSeriesId,
    val type: MatchType,
    val provider: Provider,
    val providerSeriesId: ProviderSeriesId,
    val edition: String?
)