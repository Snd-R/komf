package org.snd.mediaserver.repository

import org.snd.mediaserver.model.MatchType
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.metadata.model.Provider
import org.snd.metadata.model.ProviderSeriesId

data class SeriesMatch(
    val seriesId: MediaServerSeriesId,
    val type: MatchType,
    val provider: Provider,
    val providerSeriesId: ProviderSeriesId,
    val edition: String?
)