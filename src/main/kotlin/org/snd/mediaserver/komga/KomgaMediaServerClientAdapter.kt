package org.snd.mediaserver.komga

import org.snd.mediaserver.MediaServerClient
import org.snd.mediaserver.komga.model.dto.*
import org.snd.mediaserver.model.*
import org.snd.metadata.model.Image

class KomgaMediaServerClientAdapter(private val komgaClient: KomgaClient) : MediaServerClient {

    override fun getSeries(seriesId: MediaServerSeriesId): MediaServerSeries {
        return komgaClient.getSeries(KomgaSeriesId(seriesId.id)).mediaServerSeries()
    }

    override fun getSeries(libraryId: MediaServerLibraryId): Sequence<MediaServerSeries> {
        val komgaLibraryId = libraryId.komgaLibraryId()
        return generateSequence(komgaClient.getSeries(komgaLibraryId, 0)) {
            if (it.last) null
            else komgaClient.getSeries(komgaLibraryId, it.number + 1)
        }.flatMap { it.content }.map { it.mediaServerSeries() }
    }

    override fun getSeriesThumbnails(seriesId: MediaServerSeriesId): Collection<MediaServerSeriesThumbnail> {
        return komgaClient.getSeriesThumbnails(seriesId.komgaSeriesId())
            .map { it.mediaServerSeriesThumbnail() }
    }

    override fun getBook(bookId: MediaServerBookId): MediaServerBook {
        return komgaClient.getBook(bookId.komgaBookId()).mediaServerBook()
    }

    override fun getBooks(seriesId: MediaServerSeriesId): Collection<MediaServerBook> {
        return komgaClient.getBooks(seriesId.komgaSeriesId(), true).content
            .map { it.mediaServerBook() }
    }

    override fun getBookThumbnails(bookId: MediaServerBookId): Collection<MediaServerBookThumbnail> {
        return komgaClient.getBookThumbnails(bookId.komgaBookId())
            .map { it.mediaServerBookThumbnail() }
    }

    override fun getLibrary(libraryId: MediaServerLibraryId): MediaServerLibrary {
        return komgaClient.getLibrary(libraryId.komgaLibraryId()).mediaServerLibrary()
    }

    override fun searchSeries(name: String): Collection<MediaServerSeriesSearch> {
        return komgaClient.searchSeries(name, 0, 500).content.map { it.mediaServerSeriesSearch() }
    }

    override fun updateSeriesMetadata(seriesId: MediaServerSeriesId, metadata: MediaServerSeriesMetadataUpdate) {
        komgaClient.updateSeriesMetadata(seriesId.komgaSeriesId(), metadata.metadataUpdateRequest())
    }

    override fun deleteSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnailId: MediaServerThumbnailId) {
        komgaClient.deleteSeriesThumbnail(seriesId.komgaSeriesId(), thumbnailId.komgaThumbnailId())
    }

    override fun updateBookMetadata(bookId: MediaServerBookId, metadata: MediaServerBookMetadataUpdate) {
        komgaClient.updateBookMetadata(bookId.komgaBookId(), metadata.komgaMetadataUpdate())
    }

    override fun deleteBookThumbnail(bookId: MediaServerBookId, thumbnailId: MediaServerThumbnailId) {
        komgaClient.deleteBookThumbnail(bookId.komgaBookId(), thumbnailId.komgaThumbnailId())
    }

    override fun resetBookMetadata(bookId: MediaServerBookId, bookName: String) {
        komgaClient.updateBookMetadata(bookId.komgaBookId(), bookMetadataResetRequest(bookName), true)
    }

    override fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String) {
        komgaClient.updateSeriesMetadata(seriesId.komgaSeriesId(), metadataResetRequest(seriesName), true)
    }

    override fun uploadSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnail: Image, selected: Boolean): MediaServerSeriesThumbnail {
        return komgaClient.uploadSeriesThumbnail(seriesId.komgaSeriesId(), thumbnail, selected).mediaServerSeriesThumbnail()
    }

    override fun uploadBookThumbnail(bookId: MediaServerBookId, thumbnail: Image, selected: Boolean): MediaServerBookThumbnail {
        return komgaClient.uploadBookThumbnail(bookId.komgaBookId(), thumbnail, selected).mediaServerBookThumbnail()
    }

    override fun refreshMetadata(seriesId: MediaServerSeriesId) {
        komgaClient.analyzeSeries(seriesId.komgaSeriesId())
    }

}