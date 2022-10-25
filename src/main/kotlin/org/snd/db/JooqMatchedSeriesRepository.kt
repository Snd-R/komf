package org.snd.db

import org.jooq.DSLContext
import org.snd.jooq.Tables.MATCHED_SERIES
import org.snd.jooq.tables.records.MatchedSeriesRecord
import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.mediaserver.model.MediaServerThumbnailId
import org.snd.mediaserver.repository.MatchedSeries
import org.snd.mediaserver.repository.MatchedSeriesRepository

class JooqMatchedSeriesRepository(
    private val dsl: DSLContext,
) : MatchedSeriesRepository {

    override fun findFor(seriesId: MediaServerSeriesId, type: MediaServer): MatchedSeries? {
        return dsl.selectFrom(MATCHED_SERIES)
            .where(MATCHED_SERIES.SERIES_ID.eq(seriesId.id))
            .and(MATCHED_SERIES.SERVER_TYPE.eq(type.name))
            .fetchOne()
            ?.toModel()
    }

    override fun save(matchedSeries: MatchedSeries) {
        val record = matchedSeries.toRecord()
        dsl.insertInto(MATCHED_SERIES, *MATCHED_SERIES.fields())
            .values(record)
            .onConflict()
            .doUpdate()
            .set(record)
            .execute()
    }

    override fun delete(seriesId: MediaServerSeriesId, type: MediaServer) {
        dsl.delete(MATCHED_SERIES)
            .where(MATCHED_SERIES.SERIES_ID.eq(seriesId.id))
            .and(MATCHED_SERIES.SERVER_TYPE.eq(type.name))
            .execute()
    }

    private fun MatchedSeriesRecord.toModel(): MatchedSeries = MatchedSeries(
        seriesId = MediaServerSeriesId(seriesId),
        type = MediaServer.valueOf(serverType),
        thumbnailId = thumbnailId?.let { MediaServerThumbnailId(it) },
    )

    private fun MatchedSeries.toRecord(): MatchedSeriesRecord = MatchedSeriesRecord(
        seriesId.id,
        type.name,
        thumbnailId?.id
    )
}
