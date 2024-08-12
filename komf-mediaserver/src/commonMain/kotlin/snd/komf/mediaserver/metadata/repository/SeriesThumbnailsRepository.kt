package snd.komf.mediaserver.metadata.repository

import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerThumbnailId
import snd.komf.mediaserver.repository.SeriesThumbnail
import snd.komf.mediaserver.repository.SeriesThumbnailQueries

class SeriesThumbnailsRepository(
    private val queries: SeriesThumbnailQueries,
    private val mediaServer: MediaServer
) {

    fun findFor(seriesId: MediaServerSeriesId): SeriesThumbnail? {
        return queries.findFor(seriesId).executeAsOneOrNull()
    }

    fun save(
        seriesId: MediaServerSeriesId,
        thumbnailId: MediaServerThumbnailId?,
    ) {
        queries.save(
            seriesId = seriesId,
            thumbnailId = thumbnailId,
            mediaServer = mediaServer
        )
    }

    fun delete(seriesId: MediaServerSeriesId) {
        queries.delete(seriesId)
    }
}