package org.snd.mediaserver.repository

import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServerBookId

interface BookThumbnailsRepository {

    fun findFor(bookId: MediaServerBookId, type: MediaServer): BookThumbnail?

    fun save(bookThumbnail: BookThumbnail)

    fun delete(bookId: MediaServerBookId, type: MediaServer)
}
