package org.snd.metadata.providers.bookwalker.model

import org.snd.metadata.model.Provider.BOOK_WALKER
import org.snd.metadata.model.SeriesSearchResult

data class BookWalkerSearchResult(
    val seriesId: BookWalkerSeriesId?,
    val bookId: BookWalkerBookId?,
    val seriesName: String,
    val imageUrl: String?,
)

fun BookWalkerSearchResult.toSeriesSearchResult(seriesId: BookWalkerSeriesId): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl = imageUrl,
        title = seriesName,
        provider = BOOK_WALKER,
        resultId = seriesId.id
    )
}
