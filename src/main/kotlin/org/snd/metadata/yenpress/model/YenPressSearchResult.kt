package org.snd.metadata.yenpress.model

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
        title = title,
        provider = YEN_PRESS,
        resultId = id.id
    )
}
