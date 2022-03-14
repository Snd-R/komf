package org.snd.komga.repository

import org.snd.komga.model.MatchedSeries
import org.snd.komga.model.dto.SeriesId

interface MatchedSeriesRepository {

    fun findFor(seriesId: SeriesId): MatchedSeries?

    fun insert(matchedSeries: MatchedSeries)

    fun update(matchedSeries: MatchedSeries)
}
