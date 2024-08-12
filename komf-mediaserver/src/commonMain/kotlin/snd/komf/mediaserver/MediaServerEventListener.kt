package snd.komf.mediaserver

import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeriesId

interface MediaServerEventListener {
    suspend fun onBooksAdded(events: List<BookEvent>)
    suspend fun onBooksDeleted(events: List<BookEvent>) {}
    suspend fun onSeriesDeleted(events: List<SeriesEvent>) {}
}

data class SeriesEvent(
    val libraryId: MediaServerLibraryId,
    val seriesId: MediaServerSeriesId,
)

data class BookEvent(
    val libraryId: MediaServerLibraryId,
    val seriesId: MediaServerSeriesId,
    val bookId: MediaServerBookId,
)
