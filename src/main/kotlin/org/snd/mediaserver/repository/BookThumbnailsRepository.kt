package org.snd.mediaserver.repository

import org.snd.mediaserver.model.BookThumbnail
import org.snd.mediaserver.model.mediaserver.MediaServerBookId

interface BookThumbnailsRepository {

    fun findFor(bookId: MediaServerBookId): BookThumbnail?

    fun save(bookThumbnail: BookThumbnail)

    fun delete(bookId: MediaServerBookId)
}
