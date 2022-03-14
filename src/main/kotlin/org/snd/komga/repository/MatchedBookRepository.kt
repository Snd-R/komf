package org.snd.komga.repository

import org.snd.komga.model.MatchedBook
import org.snd.komga.model.dto.BookId

interface MatchedBookRepository {

    fun findFor(bookId: BookId): MatchedBook?

    fun insert(matchedBook: MatchedBook)

    fun update(matchedBook: MatchedBook)
}
