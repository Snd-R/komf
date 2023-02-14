package org.snd.db

import org.jooq.DSLContext
import org.snd.jooq.Tables.SERIES_THUMBNAILS
import org.snd.jooq.tables.records.SeriesThumbnailsRecord
import org.snd.mediaserver.model.SeriesThumbnail
import org.snd.mediaserver.model.mediaserver.MediaServer.KOMGA
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId
import org.snd.mediaserver.model.mediaserver.MediaServerThumbnailId
import org.snd.mediaserver.repository.SeriesThumbnailsRepository

class JooqKomgaSeriesThumbnailsRepository(
    private val dsl: DSLContext,
) : SeriesThumbnailsRepository {

    override fun findFor(seriesId: MediaServerSeriesId): SeriesThumbnail? {
        return dsl.selectFrom(SERIES_THUMBNAILS)
            .where(SERIES_THUMBNAILS.SERIES_ID.eq(seriesId.id))
            .and(SERIES_THUMBNAILS.SERVER_TYPE.eq(KOMGA.name))
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

    override fun delete(seriesId: MediaServerSeriesId) {
        dsl.delete(SERIES_THUMBNAILS)
            .where(SERIES_THUMBNAILS.SERIES_ID.eq(seriesId.id))
            .and(SERIES_THUMBNAILS.SERVER_TYPE.eq(KOMGA.name))
            .execute()
    }

    private fun SeriesThumbnailsRecord.toModel(): SeriesThumbnail = SeriesThumbnail(
        seriesId = MediaServerSeriesId(seriesId),
        thumbnailId = thumbnailId?.let { MediaServerThumbnailId(it) },
    )

    private fun SeriesThumbnail.toRecord() = SeriesThumbnailsRecord(
        seriesId.id,
        KOMGA.name,
        thumbnailId?.id
    )
}
