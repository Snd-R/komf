package snd.komf.mediaserver.match.repository

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import snd.komf.mediaserver.match.BookThumbnailMatch
import snd.komf.mediaserver.match.repository.tables.BookThumbnailTable
import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerThumbnailId

class BookThumbnailsRepository(
    private val database: Database,
    private val mediaServer: MediaServer
) {

    fun findFor(bookId: MediaServerBookId): BookThumbnailMatch? {
        return transaction(database) {
            BookThumbnailTable.selectAll()
                .where { BookThumbnailTable.bookId.eq(bookId.value) }
                .firstOrNull()
                ?.toModel()
        }
    }

    fun save(
        bookId: MediaServerBookId,
        seriesId: MediaServerSeriesId,
        thumbnailId: MediaServerThumbnailId,
    ) {
        transaction(database) {
            BookThumbnailTable.upsert {
                it[BookThumbnailTable.bookId] = bookId.value
                it[BookThumbnailTable.seriesId] = seriesId.value
                it[BookThumbnailTable.thumbnailId] = thumbnailId.value
                it[BookThumbnailTable.mediaServer] = this@BookThumbnailsRepository.mediaServer.name
            }
        }
    }

    fun delete(bookId: MediaServerBookId) {
        transaction(database) {
            BookThumbnailTable.deleteWhere { BookThumbnailTable.bookId.eq(bookId.value) }
        }
    }

    private fun ResultRow.toModel(): BookThumbnailMatch {
        return BookThumbnailMatch(
            bookId = MediaServerBookId(this[BookThumbnailTable.bookId]),
            seriesId = MediaServerSeriesId(this[BookThumbnailTable.seriesId]),
            thumbnailId = MediaServerThumbnailId(this[BookThumbnailTable.thumbnailId]),
            mediaServer = MediaServer.valueOf(this[BookThumbnailTable.mediaServer])
        )
    }
}