package org.snd.db

import org.jooq.DSLContext
import org.snd.jooq.Tables.SERIES_THUMBNAILS
import org.snd.jooq.tables.records.SeriesThumbnailsRecord
import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.mediaserver.model.MediaServerThumbnailId
import org.snd.mediaserver.repository.SeriesThumbnail
import org.snd.mediaserver.repository.SeriesThumbnailsRepository

class JooqSeriesThumbnailsRepository(
    private val dsl: DSLContext,
) : SeriesThumbnailsRepository {

    override fun findFor(seriesId: MediaServerSeriesId, type: MediaServer): SeriesThumbnail? {
        return dsl.selectFrom(SERIES_THUMBNAILS)
            .where(SERIES_THUMBNAILS.SERIES_ID.eq(seriesId.id))
            .and(SERIES_THUMBNAILS.SERVER_TYPE.eq(type.name))
            .fetchOne()
            ?.toModel()
    }

    override fun save(seriesThumbnail: SeriesThumbnail) {
        val record = seriesThumbnail.toRecord()
        dsl.insertInto(SERIES_THUMBNAILS, *SERIES_THUMBNAILS.fields())
            .values(record)
            .onConflict()
            .doUpdate()
            .set(record)
            .execute()
    }

    override fun delete(seriesId: MediaServerSeriesId, type: MediaServer) {
        dsl.delete(SERIES_THUMBNAILS)
            .where(SERIES_THUMBNAILS.SERIES_ID.eq(seriesId.id))
            .and(SERIES_THUMBNAILS.SERVER_TYPE.eq(type.name))
            .execute()
    }

    private fun SeriesThumbnailsRecord.toModel(): SeriesThumbnail = SeriesThumbnail(
        seriesId = MediaServerSeriesId(seriesId),
        type = MediaServer.valueOf(serverType),
        thumbnailId = thumbnailId?.let { MediaServerThumbnailId(it) },
    )

    private fun SeriesThumbnail.toRecord() = SeriesThumbnailsRecord(
        seriesId.id,
        type.name,
        thumbnailId?.id
    )
}
