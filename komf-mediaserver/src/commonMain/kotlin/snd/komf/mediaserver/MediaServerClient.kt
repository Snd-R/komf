package snd.komf.mediaserver

import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerBookMetadataUpdate
import snd.komf.mediaserver.model.MediaServerBookThumbnail
import snd.komf.mediaserver.model.MediaServerLibrary
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeries
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerSeriesMetadataUpdate
import snd.komf.mediaserver.model.MediaServerSeriesThumbnail
import snd.komf.mediaserver.model.MediaServerThumbnailId
import snd.komf.mediaserver.model.Page
import snd.komf.model.Image

interface MediaServerClient {
    suspend fun getSeries(seriesId: MediaServerSeriesId): MediaServerSeries
    suspend fun getSeries(libraryId: MediaServerLibraryId, pageNumber: Int): Page<MediaServerSeries>
    suspend fun getSeriesThumbnail(seriesId: MediaServerSeriesId): Image?
    suspend fun getSeriesThumbnails(seriesId: MediaServerSeriesId): Collection<MediaServerSeriesThumbnail>
    suspend fun getBook(bookId: MediaServerBookId): MediaServerBook
    suspend fun getBooks(seriesId: MediaServerSeriesId): Collection<MediaServerBook>
    suspend fun getBookThumbnails(bookId: MediaServerBookId): Collection<MediaServerBookThumbnail>
    suspend fun getBookThumbnail(bookId: MediaServerBookId): Image?
    suspend fun getLibrary(libraryId: MediaServerLibraryId): MediaServerLibrary

    suspend fun updateSeriesMetadata(seriesId: MediaServerSeriesId, metadata: MediaServerSeriesMetadataUpdate)
    suspend fun deleteSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnailId: MediaServerThumbnailId)
    suspend fun updateBookMetadata(bookId: MediaServerBookId, metadata: MediaServerBookMetadataUpdate)
    suspend fun deleteBookThumbnail(bookId: MediaServerBookId, thumbnailId: MediaServerThumbnailId)

    suspend fun resetBookMetadata(bookId: MediaServerBookId, bookName: String, bookNumber: Int?)
    suspend fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String)

    suspend fun uploadSeriesThumbnail(
        seriesId: MediaServerSeriesId,
        thumbnail: Image,
        selected: Boolean = false,
        lock: Boolean = false
    ): MediaServerSeriesThumbnail?

    suspend fun uploadBookThumbnail(
        bookId: MediaServerBookId,
        thumbnail: Image,
        selected: Boolean = false,
        lock: Boolean = false
    ): MediaServerBookThumbnail?

    suspend fun refreshMetadata(libraryId: MediaServerLibraryId, seriesId: MediaServerSeriesId)

}
