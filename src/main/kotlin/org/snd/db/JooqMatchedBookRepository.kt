package org.snd.db

import org.jooq.DSLContext
import org.snd.jooq.Tables.MATCHED_BOOKS
import org.snd.jooq.tables.records.MatchedBooksRecord
import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServerBookId
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.mediaserver.model.MediaServerThumbnailId
import org.snd.mediaserver.repository.MatchedBook
import org.snd.mediaserver.repository.MatchedBookRepository

class JooqMatchedBookRepository(
    private val dsl: DSLContext,
) : MatchedBookRepository {

    override fun findFor(bookId: MediaServerBookId, type: MediaServer): MatchedBook? {
        return dsl.selectFrom(MATCHED_BOOKS)
            .where(MATCHED_BOOKS.BOOK_ID.eq(bookId.id))
            .and(MATCHED_BOOKS.SERVER_TYPE.eq(type.name))
            .fetchOne()
            ?.toModel()
    }

    override fun save(matchedBook: MatchedBook) {
        val record = matchedBook.toRecord()
        dsl.insertInto(MATCHED_BOOKS, *MATCHED_BOOKS.fields())
            .values(record)
            .onConflict()
            .doUpdate()
            .set(record)
            .execute()
    }

    override fun delete(bookId: MediaServerBookId, type: MediaServer) {
        dsl.delete(MATCHED_BOOKS)
            .where(MATCHED_BOOKS.BOOK_ID.eq(bookId.id))
            .and(MATCHED_BOOKS.SERVER_TYPE.eq(type.name))
            .execute()
    }

    private fun MatchedBooksRecord.toModel(): MatchedBook = MatchedBook(
        bookId = MediaServerBookId(bookId),
        type = MediaServer.valueOf(serverType),
        seriesId = MediaServerSeriesId(seriesId),
        thumbnailId = thumbnailId?.let { MediaServerThumbnailId(it) },
    )

    private fun MatchedBook.toRecord(): MatchedBooksRecord = MatchedBooksRecord(
        bookId.id,
        type.name,
        seriesId.id,
        thumbnailId?.id,
    )
}
