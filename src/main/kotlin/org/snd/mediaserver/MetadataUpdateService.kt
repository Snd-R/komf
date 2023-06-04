package org.snd.mediaserver

import mu.KotlinLogging
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator
import org.snd.mediaserver.model.BookThumbnail
import org.snd.mediaserver.model.SeriesAndBookMetadata
import org.snd.mediaserver.model.SeriesThumbnail
import org.snd.mediaserver.model.UpdateMode
import org.snd.mediaserver.model.UpdateMode.API
import org.snd.mediaserver.model.UpdateMode.COMIC_INFO
import org.snd.mediaserver.model.UpdateMode.OPF
import org.snd.mediaserver.model.mediaserver.MediaServerBook
import org.snd.mediaserver.model.mediaserver.MediaServerBookId
import org.snd.mediaserver.model.mediaserver.MediaServerLibraryId
import org.snd.mediaserver.model.mediaserver.MediaServerSeries
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId
import org.snd.mediaserver.model.mediaserver.MediaServerThumbnailId
import org.snd.mediaserver.repository.BookThumbnailsRepository
import org.snd.mediaserver.repository.SeriesThumbnailsRepository
import org.snd.metadata.BookNameParser
import org.snd.metadata.comicinfo.ComicInfoWriter
import org.snd.metadata.epub.CalibreEpubMetadataWriter
import org.snd.metadata.model.Image
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.SeriesMetadata
import java.nio.file.Path
import kotlin.math.floor

private val logger = KotlinLogging.logger {}

class MetadataUpdateService(
    private val mediaServerClient: MediaServerClient,
    private val seriesThumbnailsRepository: SeriesThumbnailsRepository,
    private val bookThumbnailsRepository: BookThumbnailsRepository,
    private val metadataUpdateMapper: MetadataUpdateMapper,
    private val postProcessor: MetadataPostProcessor,
    private val comicInfoWriter: ComicInfoWriter,
    private val epubWriter: CalibreEpubMetadataWriter,

    private val updateModes: Set<UpdateMode>,
    private val overrideExistingCovers: Boolean,
    private val uploadBookCovers: Boolean,
    private val uploadSeriesCovers: Boolean,
) {
    private val requireMetadataRefresh = setOf(COMIC_INFO, OPF)
    private val natSortComparator: Comparator<String> = CaseInsensitiveSimpleNaturalComparator.getInstance()

    fun updateMetadata(series: MediaServerSeries, metadata: SeriesAndBookMetadata) {
        val processedMetadata = postProcessor.process(metadata)
        updateSeriesMetadata(series, processedMetadata.seriesMetadata)
        updateBookMetadata(unprocessedMetadata = metadata, processedMetadata = processedMetadata)

        if (updateModes.any { it in requireMetadataRefresh })
            mediaServerClient.refreshMetadata(series.libraryId, series.id)
    }

    private fun updateSeriesMetadata(series: MediaServerSeries, metadata: SeriesMetadata) {
        updateModes.forEach {
            when (it) {
                API -> {
                    logger.info { "updating series ${series.name}" }
                    val metadataUpdate = metadataUpdateMapper.toSeriesMetadataUpdate(metadata, series.metadata)
                    mediaServerClient.updateSeriesMetadata(series.id, metadataUpdate)
                }

                COMIC_INFO, OPF -> {}
            }
        }

        val newThumbnail = if (uploadSeriesCovers) metadata.thumbnail else null
        val thumbnailId = replaceSeriesThumbnail(series.id, newThumbnail)

        if (thumbnailId == null) {
            seriesThumbnailsRepository.delete(series.id)
        } else {
            seriesThumbnailsRepository.save(
                SeriesThumbnail(
                    seriesId = series.id,
                    thumbnailId = thumbnailId,
                )
            )
        }
    }

    private fun updateBookMetadata(unprocessedMetadata: SeriesAndBookMetadata, processedMetadata: SeriesAndBookMetadata) {
        val bookIdToWriteSeriesMetadata = bookToWriteSeriesMetadata(unprocessedMetadata.bookMetadata)

        processedMetadata.bookMetadata.forEach { (book, metadata) ->
            updateBookMetadata(
                book,
                metadata,
                processedMetadata.seriesMetadata,
                book.id == bookIdToWriteSeriesMetadata
            )
        }
    }

    private fun updateBookMetadata(
        book: MediaServerBook,
        metadata: BookMetadata?,
        seriesMeta: SeriesMetadata,
        writeSeriesMetadata: Boolean
    ) {
        logger.info { "updating book ${book.name}" }
        updateModes.forEach { mode ->
            when (mode) {
                API -> metadataUpdateMapper.toBookMetadataUpdate(metadata, seriesMeta, book)
                    .let { mediaServerClient.updateBookMetadata(book.id, it) }


                COMIC_INFO -> {
                    if (book.deleted) return@forEach

                    val comicInfo =
                        if (writeSeriesMetadata) metadataUpdateMapper.toSeriesComicInfo(seriesMeta, metadata)
                        else metadataUpdateMapper.toComicInfo(metadata, seriesMeta)

                    comicInfo?.let { comicInfoWriter.writeMetadata(Path.of(book.url), it) }
                }

                OPF -> {
                    if (book.deleted) return@forEach

                    if (writeSeriesMetadata) epubWriter.writeSeriesMetadata(Path.of(book.url), seriesMeta, metadata)
                    else epubWriter.writeMetadata(Path.of(book.url), seriesMeta, metadata)
                }
            }
        }

        val newThumbnail = if (uploadBookCovers) metadata?.thumbnail else null
        val thumbnailId = replaceBookThumbnail(book.id, newThumbnail)

        if (thumbnailId == null) {
            bookThumbnailsRepository.delete(book.id)
        } else {
            bookThumbnailsRepository.save(
                BookThumbnail(
                    seriesId = book.seriesId,
                    bookId = book.id,
                    thumbnailId = thumbnailId,
                )
            )
        }
    }

    private fun replaceBookThumbnail(bookId: MediaServerBookId, thumbnail: Image?): MediaServerThumbnailId? {
        val existingMatch = bookThumbnailsRepository.findFor(bookId)
        val thumbnails = mediaServerClient.getBookThumbnails(bookId)

        val selectThumbnail = overrideExistingCovers ||
                thumbnails.all { it.type == "GENERATED" || it.id == existingMatch?.thumbnailId }

        val uploadedThumbnail = thumbnail?.let {
            mediaServerClient.uploadBookThumbnail(
                bookId = bookId,
                thumbnail = thumbnail,
                selected = selectThumbnail
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
        val matchedSeries = seriesThumbnailsRepository.findFor(seriesId)
        val thumbnails = mediaServerClient.getSeriesThumbnails(seriesId)

        val selectThumbnail = overrideExistingCovers || thumbnails.isEmpty()

        val uploadedThumbnail = thumbnail?.let {
            mediaServerClient.uploadSeriesThumbnail(
                seriesId = seriesId,
                thumbnail = thumbnail,
                selected = selectThumbnail
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
            .sortedWith(compareBy(natSortComparator) { it.name })
            .forEachIndexed { index, book -> resetBookMetadata(book, index + 1) }

        replaceSeriesThumbnail(series.id, null)
        seriesThumbnailsRepository.delete(series.id)
    }

    private fun resetBookMetadata(book: MediaServerBook, sortNumber: Int?) {
        mediaServerClient.resetBookMetadata(book.id, book.name, sortNumber)

        replaceBookThumbnail(book.id, null)
        bookThumbnailsRepository.delete(book.id)
    }

    private fun bookToWriteSeriesMetadata(bookMetadata: Map<MediaServerBook, BookMetadata?>): MediaServerBookId? {
        if (updateModes.none { it == COMIC_INFO || it == OPF }) return null

        val books = bookMetadata.keys.sortedWith(compareBy(natSortComparator) { it.name })
        val firstBook = books.asSequence()
            .mapNotNull { book -> BookNameParser.getVolumes(book.name)?.let { book to it } }
            .map { (book, range) -> book to range.start }
            .filter { (_, number) -> floor(number) == number && number.toInt() == 1 }
            .map { (book, _) -> book }
            .firstOrNull()

        return firstBook?.id
            ?: if (bookMetadata.any { it.value != null }) null
            else books.firstOrNull()?.id
    }
}