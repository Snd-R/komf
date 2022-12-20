package org.snd.mediaserver.repository

import org.snd.mediaserver.model.MediaServerBookId

interface BookThumbnailsRepository {

    fun findFor(bookId: MediaServerBookId): BookThumbnail?

    fun save(bookThumbnail: BookThumbnail)

    fun delete(bookId: MediaServerBookId)
}
