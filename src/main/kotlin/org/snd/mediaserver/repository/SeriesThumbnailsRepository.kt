package org.snd.mediaserver.repository

import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServerSeriesId

interface SeriesThumbnailsRepository {

    fun findFor(seriesId: MediaServerSeriesId, type: MediaServer): SeriesThumbnail?

    fun save(seriesThumbnail: SeriesThumbnail)

    fun delete(seriesId: MediaServerSeriesId, type: MediaServer)
}
