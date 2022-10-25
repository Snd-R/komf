package org.snd.mediaserver.repository

import org.snd.mediaserver.model.MediaServer
import org.snd.mediaserver.model.MediaServerBookId

interface MatchedBookRepository {

    fun findFor(bookId: MediaServerBookId, type: MediaServer): MatchedBook?

    fun save(matchedBook: MatchedBook)

    fun delete(bookId: MediaServerBookId, type: MediaServer)
}
