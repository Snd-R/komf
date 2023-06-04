package org.snd.mediaserver

import org.snd.mediaserver.model.mediaserver.MediaServerBook
import org.snd.mediaserver.model.mediaserver.MediaServerBookId
import org.snd.mediaserver.model.mediaserver.MediaServerBookMetadataUpdate
import org.snd.mediaserver.model.mediaserver.MediaServerBookThumbnail
import org.snd.mediaserver.model.mediaserver.MediaServerLibrary
import org.snd.mediaserver.model.mediaserver.MediaServerLibraryId
import org.snd.mediaserver.model.mediaserver.MediaServerSeries
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesMetadataUpdate
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesSearch
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesThumbnail
import org.snd.mediaserver.model.mediaserver.MediaServerThumbnailId
import org.snd.metadata.model.Image

interface MediaServerClient {
    fun getSeries(seriesId: MediaServerSeriesId): MediaServerSeries
    fun getSeries(libraryId: MediaServerLibraryId): Sequence<MediaServerSeries>
    fun getSeriesThumbnail(seriesId: MediaServerSeriesId): Image?
    fun getSeriesThumbnails(seriesId: MediaServerSeriesId): Collection<MediaServerSeriesThumbnail>
    fun getBook(bookId: MediaServerBookId): MediaServerBook
    fun getBooks(seriesId: MediaServerSeriesId): Collection<MediaServerBook>
    fun getBookThumbnails(bookId: MediaServerBookId): Collection<MediaServerBookThumbnail>
    fun getBookThumbnail(bookId: MediaServerBookId): Image?
    fun getLibrary(libraryId: MediaServerLibraryId): MediaServerLibrary
    fun searchSeries(name: String): Collection<MediaServerSeriesSearch>

    fun updateSeriesMetadata(seriesId: MediaServerSeriesId, metadata: MediaServerSeriesMetadataUpdate)
    fun deleteSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnailId: MediaServerThumbnailId)
    fun updateBookMetadata(bookId: MediaServerBookId, metadata: MediaServerBookMetadataUpdate)
    fun deleteBookThumbnail(bookId: MediaServerBookId, thumbnailId: MediaServerThumbnailId)

    fun resetBookMetadata(bookId: MediaServerBookId, bookName: String, bookNumber: Int?)
    fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String)

    fun uploadSeriesThumbnail(
        seriesId: MediaServerSeriesId,
        thumbnail: Image,
        selected: Boolean = false
    ): MediaServerSeriesThumbnail?

    fun uploadBookThumbnail(
        bookId: MediaServerBookId,
        thumbnail: Image,
        selected: Boolean = false
    ): MediaServerBookThumbnail?

    fun refreshMetadata(libraryId: MediaServerLibraryId, seriesId: MediaServerSeriesId)

}
