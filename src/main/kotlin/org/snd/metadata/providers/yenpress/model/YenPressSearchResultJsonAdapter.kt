package org.snd.metadata.providers.yenpress.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class YenPressSearchResultJsonAdapter {
    @FromJson
    fun fromJson(json: YenPressSearchResultJson): YenPressSearchResult {
        return YenPressSearchResult(
            id = YenPressSeriesId(json.url.raw.removePrefix("/series/")),
            title = json.title.raw,
            imageUrl = json.image?.raw
        )
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") searchResult: YenPressSearchResult): YenPressSearchResultJson {
        throw UnsupportedOperationException()
    }
}