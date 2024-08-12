package snd.komf.mediaserver.metadata.repository

import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerThumbnailId
import snd.komf.mediaserver.repository.BookThumbnail
import snd.komf.mediaserver.repository.BookThumbnailQueries

class BookThumbnailsRepository(
    private val queries: BookThumbnailQueries,
    private val mediaServer: MediaServer
) {

    fun findFor(bookId: MediaServerBookId): BookThumbnail? {
        return queries.findFor(bookId).executeAsOneOrNull()
    }

    fun save(
        bookId: MediaServerBookId,
        seriesId: MediaServerSeriesId,
        thumbnailId: MediaServerThumbnailId?,
    ) {
        queries.save(
            bookId = bookId,
            seriesId = seriesId,
            thumbnailId = thumbnailId,
            mediaServer = mediaServer
        )
    }

    fun delete(bookId: MediaServerBookId) {
        queries.delete(bookId)
    }
}