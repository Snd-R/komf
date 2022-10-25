package org.snd.mediaserver.repository

import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServerSeriesId

interface MatchedSeriesRepository {

    fun findFor(seriesId: MediaServerSeriesId, type: MediaServer): MatchedSeries?

    fun save(matchedSeries: MatchedSeries)

    fun delete(seriesId: MediaServerSeriesId, type: MediaServer)
}
