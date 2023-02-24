package org.snd.mediaserver.kavita

import org.snd.mediaserver.MediaServerClient
import org.snd.mediaserver.kavita.model.kavitaChapterId
import org.snd.mediaserver.kavita.model.kavitaCoverResetRequest
import org.snd.mediaserver.kavita.model.kavitaLibraryId
import org.snd.mediaserver.kavita.model.kavitaSeriesId
import org.snd.mediaserver.kavita.model.kavitaSeriesMetadataUpdate
import org.snd.mediaserver.kavita.model.kavitaSeriesResetRequest
import org.snd.mediaserver.kavita.model.kavitaTitleUpdate
import org.snd.mediaserver.kavita.model.mediaServerBook
import org.snd.mediaserver.kavita.model.mediaServerLibrary
import org.snd.mediaserver.kavita.model.mediaServerSeries
import org.snd.mediaserver.kavita.model.mediaServerSeriesSearch
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
import org.snd.metadata.model.metadata.TitleType.LOCALIZED

class KavitaMediaServerClientAdapter(private val kavitaClient: KavitaClient) : MediaServerClient {

    override fun getSeries(seriesId: MediaServerSeriesId): MediaServerSeries {
        val series = kavitaClient.getSeries(seriesId.kavitaSeriesId())
        val metadata = kavitaClient.getSeriesMetadata(seriesId.kavitaSeriesId())
        return series.mediaServerSeries(metadata)
    }

    override fun getSeries(libraryId: MediaServerLibraryId): Sequence<MediaServerSeries> {
        val kavitaLibraryId = libraryId.kavitaLibraryId()

        return generateSequence(kavitaClient.getSeries(kavitaLibraryId, 1)) {
            if (it.content.isEmpty()) null
            else kavitaClient.getSeries(kavitaLibraryId, it.currentPage + 1)
        }
            .flatMap { it.content }
            .map {
                val metadata = kavitaClient.getSeriesMetadata(it.seriesId())
                it.mediaServerSeries(metadata)
            }
    }

    override fun getSeriesThumbnail(seriesId: MediaServerSeriesId): Image? {
        return runCatching { kavitaClient.getSeriesCover(seriesId.kavitaSeriesId()) }.getOrNull()
    }

    override fun getSeriesThumbnails(seriesId: MediaServerSeriesId): Collection<MediaServerSeriesThumbnail> {
        return emptyList()
    }

    override fun getBook(bookId: MediaServerBookId): MediaServerBook {
        val chapterId = bookId.kavitaChapterId()
        val chapter = kavitaClient.getChapter(chapterId)
        val volume = kavitaClient.getVolume(chapter.volumeId())
        val metadata = kavitaClient.getChapterMetadata(chapterId)

        return chapter.mediaServerBook(volume, metadata)
    }

    override fun getBooks(seriesId: MediaServerSeriesId): Collection<MediaServerBook> {
        return kavitaClient.getVolumes(seriesId.kavitaSeriesId())
            .flatMap { volume ->
                volume.chapters.map {
                    val metadata = kavitaClient.getChapterMetadata(it.chapterId())
                    it.mediaServerBook(volume, metadata)
                }
            }
    }

    override fun getBookThumbnails(bookId: MediaServerBookId): Collection<MediaServerBookThumbnail> {
        return emptyList()
    }

    override fun getLibrary(libraryId: MediaServerLibraryId): MediaServerLibrary {
        return kavitaClient.getLibraries().first { it.libraryId() == libraryId.kavitaLibraryId() }
            .mediaServerLibrary()
    }

    override fun searchSeries(name: String): Collection<MediaServerSeriesSearch> {
        return kavitaClient.search(name).series.map { it.mediaServerSeriesSearch() }
    }

    override fun updateSeriesMetadata(seriesId: MediaServerSeriesId, metadata: MediaServerSeriesMetadataUpdate) {
        val localizedName = metadata.alternativeTitles?.find { it.type == LOCALIZED && it != metadata.title }
        if (metadata.title != null || localizedName != null) {
            val series = kavitaClient.getSeries(seriesId.kavitaSeriesId())
            kavitaClient.updateSeries(series.kavitaTitleUpdate(metadata.title?.name, metadata.titleSort?.name, localizedName?.name))
        }

        val oldMetadata = kavitaClient.getSeriesMetadata(seriesId.kavitaSeriesId())
        kavitaClient.updateSeriesMetadata(metadata.kavitaSeriesMetadataUpdate(oldMetadata))
    }

    override fun deleteSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnailId: MediaServerThumbnailId) {
        val series = kavitaClient.getSeries(seriesId.kavitaSeriesId())
        kavitaClient.updateSeries(series.kavitaCoverResetRequest())
    }

    override fun updateBookMetadata(bookId: MediaServerBookId, metadata: MediaServerBookMetadataUpdate) {}

    override fun deleteBookThumbnail(bookId: MediaServerBookId, thumbnailId: MediaServerThumbnailId) {}

    override fun resetBookMetadata(bookId: MediaServerBookId, bookName: String) {
        kavitaClient.resetChapterLock(bookId.kavitaChapterId())
    }

    override fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String) {
        val series = kavitaClient.getSeries(seriesId.kavitaSeriesId())
        kavitaClient.updateSeries(series.kavitaCoverResetRequest())
        kavitaClient.updateSeriesMetadata(kavitaSeriesResetRequest(seriesId.kavitaSeriesId()))
    }

    override fun uploadSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnail: Image, selected: Boolean): MediaServerSeriesThumbnail? {
        kavitaClient.uploadSeriesCover(seriesId.kavitaSeriesId(), thumbnail)
        return null
    }

    override fun uploadBookThumbnail(bookId: MediaServerBookId, thumbnail: Image, selected: Boolean): MediaServerBookThumbnail? {
        kavitaClient.uploadBookCover(bookId.kavitaChapterId(), thumbnail)
        return null
    }

    override fun refreshMetadata(libraryId: MediaServerLibraryId, seriesId: MediaServerSeriesId) {
        kavitaClient.scanLibrary(libraryId.kavitaLibraryId())
        kavitaClient.scanSeries(seriesId.kavitaSeriesId())
    }
}