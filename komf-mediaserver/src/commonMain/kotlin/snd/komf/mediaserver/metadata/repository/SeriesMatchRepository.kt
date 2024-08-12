package snd.komf.mediaserver.metadata.repository

import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.repository.SeriesMatch
import snd.komf.mediaserver.repository.SeriesMatchQueries
import snd.komf.model.MatchType
import snd.komf.model.ProviderSeriesId
import snd.komf.providers.CoreProviders

class SeriesMatchRepository(
    private val queries: SeriesMatchQueries,
    private val mediaServer: MediaServer,
) {

    fun findManualFor(seriesId: MediaServerSeriesId): SeriesMatch? {
        return queries.findManualFor(
            seriesId = seriesId,
            mediaServer = mediaServer
        ).executeAsOneOrNull()
    }

    fun save(
        seriesId: MediaServerSeriesId,
        type: MatchType,
        provider: CoreProviders,
        providerSeriesId: ProviderSeriesId,
    ) {
        queries.save(
            seriesId = seriesId,
            type = type,
            mediaServer = mediaServer,
            provider = provider,
            providerSeriesId = providerSeriesId
        )
    }

    fun delete(seriesId: MediaServerSeriesId) {
        queries.delete(seriesId)
    }
}