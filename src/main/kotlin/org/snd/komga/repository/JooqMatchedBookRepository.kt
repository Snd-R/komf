package org.snd.komga.repository

import org.jooq.DSLContext
import org.snd.jooq.Tables.MATCHED_BOOKS
import org.snd.jooq.tables.records.MatchedBooksRecord
import org.snd.komga.model.MatchedBook
import org.snd.komga.model.dto.BookId
import org.snd.komga.model.dto.SeriesId
import org.snd.komga.model.dto.ThumbnailId

class JooqMatchedBookRepository(
    private val dsl: DSLContext,
) : MatchedBookRepository {

    override fun findFor(bookId: BookId): MatchedBook? {
        return dsl.selectFrom(MATCHED_BOOKS)
            .where(MATCHED_BOOKS.BOOK_ID.eq(bookId.id))
            .fetchOne()
            ?.toModel()
    }

    override fun insert(matchedBook: MatchedBook) {
        dsl.executeInsert(matchedBook.toRecord())
    }

    override fun update(matchedBook: MatchedBook) {
        dsl.executeUpdate(matchedBook.toRecord())
    }

    private fun MatchedBooksRecord.toModel(): MatchedBook = MatchedBook(
        bookId = BookId(bookId),
        seriesId = SeriesId(seriesId),
        thumbnailId = thumbnailId?.let { ThumbnailId(it) },
    )

    private fun MatchedBook.toRecord(): MatchedBooksRecord = MatchedBooksRecord(
        bookId.id,
        seriesId.id,
        thumbnailId?.id,
    )
}
