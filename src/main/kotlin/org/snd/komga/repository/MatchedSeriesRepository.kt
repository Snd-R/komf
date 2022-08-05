package org.snd.komga.repository

import org.snd.komga.model.MatchedSeries
import org.snd.komga.model.dto.KomgaSeriesId

interface MatchedSeriesRepository {

    fun findFor(seriesId: KomgaSeriesId): MatchedSeries?

    fun save(matchedSeries: MatchedSeries)

    fun delete(matchedSeries: MatchedSeries)
}
