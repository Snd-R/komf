package org.snd.mediaserver.repository

import org.snd.mediaserver.model.MediaServerSeriesId

interface SeriesThumbnailsRepository {

    fun findFor(seriesId: MediaServerSeriesId): SeriesThumbnail?

    fun save(seriesThumbnail: SeriesThumbnail)

    fun delete(seriesId: MediaServerSeriesId)
}
