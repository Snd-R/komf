package org.snd.metadata.providers.bookwalker.model

import org.snd.metadata.model.BookRange

data class BookWalkerSeriesBook(
    val id: BookWalkerBookId,
    val number: BookRange?,
    val name: String
)
