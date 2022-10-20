package org.snd.metadata.providers.bookwalker.model

data class BookWalkerBookListPage(
    val page: Int,
    val totalPages: Int,
    val books: Collection<BookWalkerSeriesBook>
)

