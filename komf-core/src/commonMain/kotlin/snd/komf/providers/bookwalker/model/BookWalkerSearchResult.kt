package snd.komf.providers.bookwalker.model

import kotlin.jvm.JvmInline

@JvmInline
value class BookWalkerSeriesId(val id: String)

data class BookWalkerSearchResult(
    val seriesId: BookWalkerSeriesId?,
    val bookId: BookWalkerBookId?,
    val seriesName: String,
    val imageUrl: String?,
)

