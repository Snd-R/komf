package org.snd.metadata.providers.bookwalker.model

import org.snd.metadata.model.Provider.BOOK_WALKER
import org.snd.metadata.model.SeriesSearchResult

data class BookWalkerSearchResult(
    val id: BookWalkerSeriesId,
    val seriesName: String,
    val imageUrl: String?,
)

fun BookWalkerSearchResult.toSeriesSearchResult(): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl = imageUrl,
        title = seriesName,
        provider = BOOK_WALKER,
        resultId = id.id
    )
}
