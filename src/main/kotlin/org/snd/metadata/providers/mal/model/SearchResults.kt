package org.snd.metadata.providers.mal.model

import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult

data class SearchResults(
    val results: List<SearchResult>,
    val nextPage: String
)

data class SearchResult(
    val id: Int,
    val title: String,
    val alternativeTitles: AlternativeTitles,
    val mediaType: String,
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
