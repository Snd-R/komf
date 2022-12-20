package org.snd.db

import org.jooq.DSLContext
import org.snd.jooq.Tables.SERIES_MATCH
import org.snd.jooq.tables.records.SeriesMatchRecord
import org.snd.mediaserver.model.MatchType
import org.snd.mediaserver.model.MatchType.MANUAL
import org.snd.mediaserver.model.MediaServer.KOMGA
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.mediaserver.repository.SeriesMatch
import org.snd.mediaserver.repository.SeriesMatchRepository
import org.snd.metadata.model.Provider
import org.snd.metadata.model.ProviderSeriesId

class JooqKomgaSeriesMatchRepository(
    private val dsl: DSLContext,
) : SeriesMatchRepository {

    override fun findManualFor(seriesId: MediaServerSeriesId): SeriesMatch? {
        return dsl.selectFrom(SERIES_MATCH)
            .where(SERIES_MATCH.SERIES_ID.eq(seriesId.id))
            .and(SERIES_MATCH.TYPE.eq(MANUAL.name))
            .and(SERIES_MATCH.SERVER_TYPE.eq(KOMGA.name))
            .fetchOne()
            ?.toModel()
    }

    override fun save(match: SeriesMatch) {
        val record = match.toRecord()
        dsl.insertInto(SERIES_MATCH, *SERIES_MATCH.fields())
            .values(record)
            .onConflict()
            .doUpdate()
            .set(record)
            .execute()
    }

    override fun delete(seriesId: MediaServerSeriesId) {
        dsl.delete(SERIES_MATCH)
            .where(SERIES_MATCH.SERIES_ID.eq(seriesId.id))
            .and(SERIES_MATCH.SERVER_TYPE.eq(KOMGA.name))
            .execute()
    }

    private fun SeriesMatchRecord.toModel() = SeriesMatch(
        seriesId = MediaServerSeriesId(seriesId),
        type = MatchType.valueOf(type),
        provider = Provider.valueOf(provider),
        providerSeriesId = ProviderSeriesId(providerSeriesId),
        edition = edition
    )

    private fun SeriesMatch.toRecord() = SeriesMatchRecord(
        seriesId.id,
        type.name,
        KOMGA.name,
        provider.name,
        providerSeriesId.id,
        edition
    )
}
