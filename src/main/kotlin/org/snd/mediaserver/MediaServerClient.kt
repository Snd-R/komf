package org.snd.mediaserver

import org.snd.mediaserver.model.*
import org.snd.metadata.model.Image

interface MediaServerClient {
    fun getSeries(seriesId: MediaServerSeriesId): MediaServerSeries
    fun getSeries(libraryId: MediaServerLibraryId): Sequence<MediaServerSeries>
    fun getSeriesThumbnails(seriesId: MediaServerSeriesId): Collection<MediaServerSeriesThumbnail>
    fun getBook(bookId: MediaServerBookId): MediaServerBook
    fun getBooks(seriesId: MediaServerSeriesId): Collection<MediaServerBook>
    fun getBookThumbnails(bookId: MediaServerBookId): Collection<MediaServerBookThumbnail>
    fun getLibrary(libraryId: MediaServerLibraryId): MediaServerLibrary
    fun searchSeries(name: String): Collection<MediaServerSeries>

    fun updateSeriesMetadata(seriesId: MediaServerSeriesId, metadata: MediaServerSeriesMetadataUpdate)
    fun deleteSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnailId: MediaServerThumbnailId)
    fun updateBookMetadata(bookId: MediaServerBookId, metadata: MediaServerBookMetadataUpdate)
    fun deleteBookThumbnail(bookId: MediaServerBookId, thumbnailId: MediaServerThumbnailId)

    fun resetBookMetadata(bookId: MediaServerBookId, bookName: String)
    fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String)

    fun uploadSeriesThumbnail(
        seriesId: MediaServerSeriesId,
        thumbnail: Image,
        selected: Boolean = false
    ): MediaServerSeriesThumbnail

    fun uploadBookThumbnail(
        bookId: MediaServerBookId,
        thumbnail: Image,
        selected: Boolean = false
    ): MediaServerBookThumbnail

    fun refreshMetadata(seriesId: MediaServerSeriesId)

}
