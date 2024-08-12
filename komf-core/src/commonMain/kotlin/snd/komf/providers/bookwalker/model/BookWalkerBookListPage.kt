package snd.komf.providers.bookwalker.model

import snd.komf.model.BookRange

data class BookWalkerBookListPage(
    val page: Int,
    val totalPages: Int,
    val books: Collection<BookWalkerSeriesBook>
)

data class BookWalkerSeriesBook(
    val id: BookWalkerBookId,
    val number: BookRange?,
    val name: String
)
