package org.snd.metadata.providers.yenpress.model

import com.squareup.moshi.JsonClass
import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult

data class YenPressSearchResult(
    val id: YenPressSeriesId,
    val title: String,
    val imageUrl: String?,
)

fun YenPressSearchResult.toSeriesSearchResult(): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl = imageUrl,
        title = title,
        provider = Provider.YEN_PRESS,
        resultId = id.id
    )
}

@JsonClass(generateAdapter = true)
data class YenPressSearchResults(
    val results: List<YenPressSearchResult>
)


@JsonClass(generateAdapter = true)
data class YenPressSearchResponseJson(
    val results: List<YenPressSearchResultJson>
)

@JsonClass(generateAdapter = true)
data class YenPressSearchResultJson(
    val id: RawFieldJson,
    val image: RawFieldJson?,
    val title: RawFieldJson,
    val url: RawFieldJson,
)

@JsonClass(generateAdapter = true)
data class RawFieldJson(
    val raw: String
)