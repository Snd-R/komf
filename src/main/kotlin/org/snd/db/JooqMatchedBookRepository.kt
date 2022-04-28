package org.snd.db

import org.jooq.DSLContext
import org.snd.jooq.Tables.MATCHED_BOOKS
import org.snd.jooq.tables.records.MatchedBooksRecord
import org.snd.komga.model.MatchedBook
import org.snd.komga.model.dto.KomgaBookId
import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.komga.model.dto.KomgaThumbnailId
import org.snd.komga.repository.MatchedBookRepository

class JooqMatchedBookRepository(
    private val dsl: DSLContext,
) : MatchedBookRepository {

    override fun findFor(bookId: KomgaBookId): MatchedBook? {
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

    override fun delete(matchedBook: MatchedBook) {
        dsl.executeDelete(matchedBook.toRecord())
    }

    private fun MatchedBooksRecord.toModel(): MatchedBook = MatchedBook(
        bookId = KomgaBookId(bookId),
        seriesId = KomgaSeriesId(seriesId),
        thumbnailId = thumbnailId?.let { KomgaThumbnailId(it) },
    )

    private fun MatchedBook.toRecord(): MatchedBooksRecord = MatchedBooksRecord(
        bookId.id,
        seriesId.id,
        thumbnailId?.id,
    )
}
