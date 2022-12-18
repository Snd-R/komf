package org.snd.mediaserver

import org.snd.config.MetadataUpdateConfig
import org.snd.mediaserver.UpdateMode.API
import org.snd.mediaserver.UpdateMode.COMIC_INFO
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
import java.nio.file.Path

class MetadataUpdateService(
    private val mediaServerClient: MediaServerClient,
    private val matchedSeriesRepository: MatchedSeriesRepository,
    private val matchedBookRepository: MatchedBookRepository,
    private val metadataUpdateConfig: MetadataUpdateConfig,
    private val metadataUpdateMapper: MetadataUpdateMapper,
    private val comicInfoWriter: ComicInfoWriter,
    private val serverType: MediaServer,
) {
    private val requireMetadataRefresh = setOf(COMIC_INFO)

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

                COMIC_INFO -> {}
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
        val bookIdToWriteSeriesMetadata = bookToWriteSeriesMetadata(bookMetadata)
        bookMetadata.forEach { (book, metadata) ->
            updateBookMetadata(book, metadata, seriesMetadata, book.id == bookIdToWriteSeriesMetadata)
        }
    }

    private fun updateBookMetadata(
        book: MediaServerBook,
        metadata: BookMetadata?,
        seriesMeta: SeriesMetadata?,
        writeSeriesMetadata: Boolean
    ) {
        metadataUpdateConfig.modes.forEach { mode ->
            when (mode) {
                API -> metadataUpdateMapper.toBookMetadataUpdate(metadata, seriesMeta, book.metadata)
                    ?.let { mediaServerClient.updateBookMetadata(book.id, it) }

                COMIC_INFO -> {
                    if (book.deleted.not()) {
                        if (writeSeriesMetadata) seriesMeta?.let {
                            val comicInfo = metadataUpdateMapper.toSeriesComicInfo(it)
                            comicInfoWriter.writeMetadata(Path.of(book.url), comicInfo)
                        }
                        else metadataUpdateMapper.toComicInfo(metadata, seriesMeta)
                            ?.let { comicInfoWriter.writeMetadata(Path.of(book.url), it) }
                    }
                }
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

    private fun bookToWriteSeriesMetadata(bookMetadata: Map<MediaServerBook, BookMetadata?>): MediaServerBookId? {
        return if (metadataUpdateConfig.modes.any { it == COMIC_INFO }
            && bookMetadata.all { it.value == null }) {
            return bookMetadata.keys.asSequence()
                .mapNotNull { book -> BookFilenameParser.getVolumes(book.name)?.let { book to it } }
                .filter { (_, number) -> number.first == number.last }
                .map { (book, number) -> book to number.first }
                .filter { (_, number) -> number == 1 }
                .map { (book, _) -> book.id }
                .firstOrNull()

        } else null
    }
}