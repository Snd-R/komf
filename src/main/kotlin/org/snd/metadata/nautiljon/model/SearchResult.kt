package org.snd.metadata.nautiljon.model

import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult
import java.time.Year

data class SearchResult(
    val id: SeriesId,
    val title: String,
    val alternativeTitle: String?,
    val description: String?,
    val imageUrl: String?,
    val type: String?,
    val volumesNumber: Int?,
    val startDate: Year?,
    val score: Double?,
)

fun SearchResult.toSeriesSearchResult(): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl = imageUrl,
        title = title,
        provider = Provider.NAUTILJON,
        resultId = id.id
    )
}
