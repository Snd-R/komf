package snd.komf.mediaserver.match

import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.model.MatchType
import snd.komf.model.ProviderSeriesId
import snd.komf.providers.CoreProviders

data class SeriesMatch (
    val seriesId: MediaServerSeriesId,
    val type: MatchType,
    val mediaServer: MediaServer,
    val provider: CoreProviders,
    val providerSeriesId: ProviderSeriesId,
)