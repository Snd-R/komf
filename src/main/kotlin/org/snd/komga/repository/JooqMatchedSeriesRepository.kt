package org.snd.komga.repository

import org.jooq.DSLContext
import org.snd.jooq.Tables.MATCHED_SERIES
import org.snd.jooq.tables.records.MatchedSeriesRecord
import org.snd.komga.model.MatchedSeries
import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.komga.model.dto.KomgaThumbnailId
import org.snd.metadata.Provider
import org.snd.metadata.ProviderSeriesId

class JooqMatchedSeriesRepository(
    private val dsl: DSLContext,
) : MatchedSeriesRepository {

    override fun findFor(seriesId: KomgaSeriesId): MatchedSeries? {
        return dsl.selectFrom(MATCHED_SERIES)
            .where(MATCHED_SERIES.SERIES_ID.eq(seriesId.id))
            .fetchOne()
            ?.toModel()
    }

    override fun insert(matchedSeries: MatchedSeries) {
        dsl.executeInsert(matchedSeries.toRecord())
    }

    override fun update(matchedSeries: MatchedSeries) {
        dsl.executeUpdate(matchedSeries.toRecord())
    }

    private fun MatchedSeriesRecord.toModel(): MatchedSeries = MatchedSeries(
        seriesId = KomgaSeriesId(seriesId),
        thumbnailId = thumbnailId?.let { KomgaThumbnailId(it) },
        provider = Provider.valueOf(provider),
        providerSeriesId = ProviderSeriesId(providerSeriesId)
    )

    private fun MatchedSeries.toRecord(): MatchedSeriesRecord = MatchedSeriesRecord(
        seriesId.id,
        thumbnailId?.id,
        provider.name,
        providerSeriesId.id
    )
}
