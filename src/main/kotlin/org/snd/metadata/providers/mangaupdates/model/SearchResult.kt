package org.snd.metadata.providers.mangaupdates.model

import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult
import java.time.Year

data class SearchResult(
    val id: Long,
    val title: String,
    val summary: String?,
    val thumbnail: String?,
    val genres: Collection<String>,
    val year: Year?,
)

fun SearchResult.toSeriesSearchResult(): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl = thumbnail,
        title = title,
        provider = Provider.MANGA_UPDATES,
        resultId = id.toString()
    )
}
