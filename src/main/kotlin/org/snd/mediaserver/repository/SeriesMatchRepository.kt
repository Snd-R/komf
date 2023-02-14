package org.snd.mediaserver.repository

import org.snd.mediaserver.model.SeriesMatch
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId

interface SeriesMatchRepository {

    fun findManualFor(seriesId: MediaServerSeriesId): SeriesMatch?

    fun save(match: SeriesMatch)

    fun delete(seriesId: MediaServerSeriesId)
}
