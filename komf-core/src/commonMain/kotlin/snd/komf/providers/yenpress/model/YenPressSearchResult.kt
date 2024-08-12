package snd.komf.providers.yenpress.model

import kotlinx.serialization.Serializable

@Serializable
data class YenPressSearchResponse(
    val results: List<YenPressSearchResult>
)

@Serializable
data class YenPressSearchResult(
    val title: YenPressSearchField,
    val url: YenPressSearchField,
    val image: YenPressSearchField?,
) {
    val id: YenPressSeriesId
        get() = YenPressSeriesId(url.raw.removePrefix("/series/"))
}

@Serializable
data class YenPressSearchField(
    val raw: String
)