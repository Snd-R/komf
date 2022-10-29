package org.snd.mediaserver

import org.snd.config.MetadataUpdateConfig
import org.snd.mediaserver.UpdateMode.*
import org.snd.mediaserver.model.*
import org.snd.mediaserver.repository.MatchedBook
import org.snd.mediaserver.repository.MatchedBookRepository
import org.snd.mediaserver.repository.MatchedSeries
import org.snd.mediaserver.repository.MatchedSeriesRepository
import org.snd.metadata.BookFilenameParser
import org.snd.metadata.comicinfo.ComicInfoWriter
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.Image
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.mylar.SeriesJsonWriter
import java.nio.file.Path

class MetadataUpdateService(
    private val mediaServerClient: MediaServerClient,
    private val matchedSeriesRepository: MatchedSeriesRepository,
    private val matchedBookRepository: MatchedBookRepository,
    private val metadataUpdateConfig: MetadataUpdateConfig,
    private val metadataUpdateMapper: MetadataUpdateMapper,
    private val comicInfoWriter: ComicInfoWriter,
    private val seriesJsonWriter: SeriesJsonWriter,
    private val serverType: MediaServer,
) {
    private val requireMetadataRefresh = setOf(FILE_EMBED, COMIC_INFO, SERIES_JSON)

    fun updateMetadata(series: MediaServerSeries, metadata: SeriesAndBookMetadata) {
        if (metadata.seriesMetadata != null) updateSeriesMetadata(series, metadata.seriesMetadata)
        updateBookMetadata(metadata.bookMetadata, metadata.seriesMetadata)

        if (metadataUpdateConfig.modes.any { it in requireMetadataRefresh })
            mediaServerClient.refreshMetadata(series.id)
    }

    private fun updateSeriesMetadata(series: MediaServerSeries, metadata: SeriesMetadata) {
        metadataUpdateConfig.modes.forEach {
            when (it) {
                API -> {
                    val metadataUpdate = metadataUpdateMapper.toSeriesMetadataUpdate(metadata, series.metadata)
                    mediaServerClient.updateSeriesMetadata(series.id, metadataUpdate)
                }

                COMIC_INFO, FILE_EMBED -> {}
                SERIES_JSON -> {
                    val mylarMetadata = metadataUpdateMapper.toMylarMetadata(series, metadata)
                    seriesJsonWriter.write(Path.of(series.url), mylarMetadata)
                }
            }
        }

        val newThumbnail = if (metadataUpdateConfig.seriesThumbnails) metadata.thumbnail else null
        val thumbnailId = replaceSeriesThumbnail(series.id, newThumbnail)

        if (thumbnailId == null) {
            matchedSeriesRepository.delete(series.id, serverType)
        } else {
            matchedSeriesRepository.save(
                MatchedSeries(
                    seriesId = series.id,
                    type = serverType,
                    thumbnailId = thumbnailId,
                )
            )
        }
    }

    private fun updateBookMetadata(bookMetadata: Map<MediaServerBook, BookMetadata?>, seriesMetadata: SeriesMetadata?) {
        val filteredBooks = writeComicInfoToFirstVolume(bookMetadata, seriesMetadata)
        filteredBooks.forEach { (book, metadata) -> updateBookMetadata(book, metadata, seriesMetadata) }
    }

    private fun updateBookMetadata(book: MediaServerBook, metadata: BookMetadata?, seriesMeta: SeriesMetadata?) {
        metadataUpdateConfig.modes.forEach { mode ->
            when (mode) {
                API -> metadataUpdateMapper.toBookMetadataUpdate(metadata, seriesMeta, book.metadata)
                    ?.let { mediaServerClient.updateBookMetadata(book.id, it) }

                COMIC_INFO, FILE_EMBED -> {
                    if (book.deleted.not()) {
                        metadataUpdateMapper.toComicInfo(metadata, seriesMeta)?.let {
                            comicInfoWriter.writeMetadata(Path.of(book.url), it)
                        }
                    }
                }

                SERIES_JSON -> {}
            }
        }

        val newThumbnail = if (metadataUpdateConfig.bookThumbnails) metadata?.thumbnail else null
        val thumbnailId = replaceBookThumbnail(book.id, newThumbnail)

        if (thumbnailId == null) {
            matchedBookRepository.delete(book.id, serverType)
        } else {
            matchedBookRepository.save(
                MatchedBook(
                    seriesId = book.seriesId,
                    bookId = book.id,
                    type = serverType,
                    thumbnailId = thumbnailId,
                )
            )
        }
    }

    private fun replaceBookThumbnail(bookId: MediaServerBookId, thumbnail: Image?): MediaServerThumbnailId? {
        val existingMatch = matchedBookRepository.findFor(bookId, serverType)
        val thumbnails = mediaServerClient.getBookThumbnails(bookId)

        val uploadedThumbnail = thumbnail?.let {
            mediaServerClient.uploadBookThumbnail(
                bookId = bookId,
                thumbnail = thumbnail,
                selected = thumbnails.all { it.type == "GENERATED" || it.id == existingMatch?.thumbnailId }
            )
        }

        existingMatch?.thumbnailId?.let { thumb ->
            if (thumbnails.any { it.id == thumb }) {
                mediaServerClient.deleteBookThumbnail(bookId, thumb)
            }
        }

        return uploadedThumbnail?.id
    }

    private fun replaceSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnail: Image?): MediaServerThumbnailId? {
        val matchedSeries = matchedSeriesRepository.findFor(seriesId, serverType)
        val thumbnails = mediaServerClient.getSeriesThumbnails(seriesId)

        val uploadedThumbnail = thumbnail?.let {
            mediaServerClient.uploadSeriesThumbnail(
                seriesId = seriesId,
                thumbnail = thumbnail,
                selected = thumbnails.isEmpty()
            )
        }

        matchedSeries?.thumbnailId?.let { thumb ->
            if (thumbnails.any { it.id == thumb }) {
                mediaServerClient.deleteSeriesThumbnail(seriesId, thumb)
            }
        }

        return uploadedThumbnail?.id
    }

    fun resetLibraryMetadata(libraryId: MediaServerLibraryId) {
        mediaServerClient.getSeries(libraryId).forEach { resetSeriesMetadata(it) }
    }

    fun resetSeriesMetadata(seriesId: MediaServerSeriesId) {
        val series = mediaServerClient.getSeries(seriesId)
        resetSeriesMetadata(series)
    }


    private fun resetSeriesMetadata(series: MediaServerSeries) {
        mediaServerClient.resetSeriesMetadata(series.id, series.name)

        mediaServerClient.getBooks(series.id)
            .forEach { resetBookMetadata(it) }

        replaceSeriesThumbnail(series.id, null)
        matchedSeriesRepository.delete(series.id, serverType)
    }

    private fun resetBookMetadata(book: MediaServerBook) {
        mediaServerClient.resetBookMetadata(book.id, book.name)

        replaceBookThumbnail(book.id, null)
        matchedBookRepository.delete(book.id, serverType)
    }

    private fun writeComicInfoToFirstVolume(
        bookMetadata: Map<MediaServerBook, BookMetadata?>,
        seriesMetadata: SeriesMetadata?
    ): Map<MediaServerBook, BookMetadata?> {
        return if (metadataUpdateConfig.modes.any { it == COMIC_INFO || it == FILE_EMBED }
            && bookMetadata.all { it.value == null }
            && seriesMetadata != null
        ) {
            val firstVolume = bookMetadata.keys.asSequence()
                .mapNotNull { book -> BookFilenameParser.getVolumes(book.name)?.let { book to it } }
                .filter { (_, number) -> number.first == number.last }
                .map { (book, number) -> book to number.first }
                .filter { (_, number) -> number == 1 }
                .map { (book, _) -> book }
                .firstOrNull()

            return if (firstVolume != null) {
                val comicInfo = metadataUpdateMapper.toSeriesComicInfo(seriesMetadata)
                comicInfoWriter.writeMetadata(Path.of(firstVolume.url), comicInfo)
                bookMetadata.filter { it.key != firstVolume }
            } else bookMetadata

        } else bookMetadata
    }
}