package org.snd.metadata.providers.yenpress.model

import org.snd.metadata.model.Provider.YEN_PRESS
import org.snd.metadata.model.SeriesSearchResult

data class YenPressSearchResult(
    val id: YenPressBookId,
    val title: String,
    val imageUrl: String?,
)

fun YenPressSearchResult.toSeriesSearchResult(): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl = imageUrl,
        title = title.replace(", Vol. [0-9]+".toRegex(), ""),
        provider = YEN_PRESS,
        resultId = id.id
    )
}
