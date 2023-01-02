package org.snd.mediaserver

import org.snd.mediaserver.model.MediaServerBook
import org.snd.mediaserver.model.MediaServerBookId
import org.snd.mediaserver.model.MediaServerBookMetadataUpdate
import org.snd.mediaserver.model.MediaServerBookThumbnail
import org.snd.mediaserver.model.MediaServerLibrary
import org.snd.mediaserver.model.MediaServerLibraryId
import org.snd.mediaserver.model.MediaServerSeries
import org.snd.mediaserver.model.MediaServerSeriesId
import org.snd.mediaserver.model.MediaServerSeriesMetadataUpdate
import org.snd.mediaserver.model.MediaServerSeriesSearch
import org.snd.mediaserver.model.MediaServerSeriesThumbnail
import org.snd.mediaserver.model.MediaServerThumbnailId
import org.snd.metadata.model.Image

interface MediaServerClient {
    fun getSeries(seriesId: MediaServerSeriesId): MediaServerSeries
    fun getSeries(libraryId: MediaServerLibraryId): Sequence<MediaServerSeries>
    fun getSeriesThumbnail(seriesId: MediaServerSeriesId): ByteArray?
    fun getSeriesThumbnails(seriesId: MediaServerSeriesId): Collection<MediaServerSeriesThumbnail>
    fun getBook(bookId: MediaServerBookId): MediaServerBook
    fun getBooks(seriesId: MediaServerSeriesId): Collection<MediaServerBook>
    fun getBookThumbnails(bookId: MediaServerBookId): Collection<MediaServerBookThumbnail>
    fun getLibrary(libraryId: MediaServerLibraryId): MediaServerLibrary
    fun searchSeries(name: String): Collection<MediaServerSeriesSearch>

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
    ): MediaServerSeriesThumbnail?

    fun uploadBookThumbnail(
        bookId: MediaServerBookId,
        thumbnail: Image,
        selected: Boolean = false
    ): MediaServerBookThumbnail?

    fun refreshMetadata(seriesId: MediaServerSeriesId)

}
