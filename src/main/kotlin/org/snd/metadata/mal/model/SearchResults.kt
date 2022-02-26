package org.snd.metadata.mal.model

import org.snd.metadata.Provider
import org.snd.model.SeriesSearchResult

data class SearchResults(
    val results: List<SearchResult>,
    val nextPage: String
)

data class SearchResult(
    val id: Int,
    val title: String,
    val alternative_titles: AlternativeTitles,
    val mainPicture: Picture?,
)

fun SearchResult.toSeriesSearchResult(): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl =  mainPicture?.medium,
        title = title,
        provider = Provider.MAL,
        resultId = id.toString()
    )
}
