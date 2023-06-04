package org.snd.mediaserver.komga

import mu.KotlinLogging
import org.snd.mediaserver.MediaServerClient
import org.snd.mediaserver.komga.model.dto.KomgaSeriesId
import org.snd.mediaserver.komga.model.dto.bookMetadataResetRequest
import org.snd.mediaserver.komga.model.dto.komgaBookId
import org.snd.mediaserver.komga.model.dto.komgaLibraryId
import org.snd.mediaserver.komga.model.dto.komgaMetadataUpdate
import org.snd.mediaserver.komga.model.dto.komgaSeriesId
import org.snd.mediaserver.komga.model.dto.komgaThumbnailId
import org.snd.mediaserver.komga.model.dto.mediaServerBook
import org.snd.mediaserver.komga.model.dto.mediaServerBookThumbnail
import org.snd.mediaserver.komga.model.dto.mediaServerLibrary
import org.snd.mediaserver.komga.model.dto.mediaServerSeries
import org.snd.mediaserver.komga.model.dto.mediaServerSeriesSearch
import org.snd.mediaserver.komga.model.dto.mediaServerSeriesThumbnail
import org.snd.mediaserver.komga.model.dto.metadataResetRequest
import org.snd.mediaserver.komga.model.dto.metadataUpdateRequest
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

private val logger = KotlinLogging.logger {}

class KomgaMediaServerClientAdapter(
    private val komgaClient: KomgaClient,
    private val komgaCoverUploadLimit: Long
) : MediaServerClient {

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

    override fun getSeriesThumbnail(seriesId: MediaServerSeriesId): Image? {
        return runCatching { komgaClient.getSeriesThumbnail(seriesId.komgaSeriesId()) }.getOrNull()
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

    override fun getBookThumbnail(bookId: MediaServerBookId): Image? {
        return runCatching { komgaClient.getBookThumbnail(bookId.komgaBookId()) }.getOrNull()
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

    override fun resetBookMetadata(bookId: MediaServerBookId, bookName: String, bookNumber: Int?) {
        komgaClient.updateBookMetadata(
            bookId.komgaBookId(),
            bookMetadataResetRequest(bookName, bookNumber),
            true
        )
    }

    override fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String) {
        komgaClient.updateSeriesMetadata(seriesId.komgaSeriesId(), metadataResetRequest(seriesName), true)
    }

    override fun uploadSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnail: Image, selected: Boolean): MediaServerSeriesThumbnail? {
        if (thumbnail.image.size > komgaCoverUploadLimit) {
            logger.warn { "Thumbnail size is bigger than $komgaCoverUploadLimit bytes. Skipping thumbnail upload" }
            return null
        }

        return komgaClient.uploadSeriesThumbnail(seriesId.komgaSeriesId(), thumbnail, selected).mediaServerSeriesThumbnail()
    }

    override fun uploadBookThumbnail(bookId: MediaServerBookId, thumbnail: Image, selected: Boolean): MediaServerBookThumbnail? {
        if (thumbnail.image.size > komgaCoverUploadLimit) {
            logger.warn { "Thumbnail size is bigger than $komgaCoverUploadLimit bytes. Skipping thumbnail upload" }
            return null
        }
        return komgaClient.uploadBookThumbnail(bookId.komgaBookId(), thumbnail, selected).mediaServerBookThumbnail()
    }

    override fun refreshMetadata(libraryId: MediaServerLibraryId, seriesId: MediaServerSeriesId) {
        komgaClient.analyzeSeries(seriesId.komgaSeriesId())
    }

}