package snd.komf.mediaserver.metadata

import io.github.oshai.kotlinlogging.KotlinLogging
import snd.komf.comicinfo.ComicInfoWriter
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeries
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerThumbnailId
import snd.komf.mediaserver.model.SeriesAndBookMetadata
import snd.komf.mediaserver.metadata.repository.BookThumbnailsRepository
import snd.komf.mediaserver.metadata.repository.SeriesThumbnailsRepository
import snd.komf.model.BookMetadata
import snd.komf.model.Image
import snd.komf.model.SeriesMetadata
import snd.komf.model.UpdateMode
import snd.komf.util.BookNameParser
import snd.komf.util.caseInsensitiveNatSortComparator
import kotlin.math.floor

private val logger = KotlinLogging.logger {}

class MetadataUpdater(
    private val mediaServerClient: MediaServerClient,
    private val seriesThumbnailsRepository: SeriesThumbnailsRepository,
    private val bookThumbnailsRepository: BookThumbnailsRepository,
    private val metadataUpdateMapper: MetadataMapper,
    private val postProcessor: MetadataPostProcessor,
    private val comicInfoWriter: ComicInfoWriter,

    private val updateModes: Set<UpdateMode>,
    private val overrideExistingCovers: Boolean,
    private val uploadBookCovers: Boolean,
    private val uploadSeriesCovers: Boolean,
) {
    private val requireMetadataRefresh = setOf(UpdateMode.COMIC_INFO)
    private val natSortComparator: Comparator<String> = caseInsensitiveNatSortComparator()

    suspend fun updateMetadata(series: MediaServerSeries, metadata: SeriesAndBookMetadata) {
        val processedMetadata = postProcessor.process(metadata)
        updateSeriesMetadata(series, processedMetadata.seriesMetadata)
        updateBookMetadata(unprocessedMetadata = metadata, processedMetadata = processedMetadata)

        if (updateModes.any { it in requireMetadataRefresh })
            mediaServerClient.refreshMetadata(series.libraryId, series.id)
    }

    suspend fun resetLibraryMetadata(libraryId: MediaServerLibraryId, removeComicInfo: Boolean) {
        var pageNumber = 1
        do {
            val page = mediaServerClient.getSeries(libraryId, pageNumber)
            page.content.forEach { resetSeriesMetadata(it, removeComicInfo) }
            pageNumber++
        } while (page.pageNumber != page.totalPages || page.content.isNotEmpty())
    }

    suspend fun resetSeriesMetadata(seriesId: MediaServerSeriesId, removeComicInfo: Boolean) {
        val series = mediaServerClient.getSeries(seriesId)
        resetSeriesMetadata(series, removeComicInfo)
    }

    private suspend fun updateSeriesMetadata(series: MediaServerSeries, metadata: SeriesMetadata) {
        updateModes.forEach {
            when (it) {
                UpdateMode.API -> {
                    logger.info { "updating series ${series.name}" }
                    val metadataUpdate = metadataUpdateMapper.toSeriesMetadataUpdate(metadata, series.metadata)
                    mediaServerClient.updateSeriesMetadata(series.id, metadataUpdate)
                }

                UpdateMode.COMIC_INFO -> {}
            }
        }

        val newThumbnail = if (uploadSeriesCovers) metadata.thumbnail else null
        val thumbnailId = replaceSeriesThumbnail(series.id, newThumbnail)

        if (thumbnailId == null) {
            seriesThumbnailsRepository.delete(series.id)
        } else {
            seriesThumbnailsRepository.save(
                seriesId = series.id,
                thumbnailId = thumbnailId,
            )
        }
    }

    private suspend fun updateBookMetadata(
        unprocessedMetadata: SeriesAndBookMetadata,
        processedMetadata: SeriesAndBookMetadata
    ) {
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

    private suspend fun updateBookMetadata(
        book: MediaServerBook,
        metadata: BookMetadata?,
        seriesMeta: SeriesMetadata,
        writeSeriesMetadata: Boolean
    ) {
        logger.info { "updating book ${book.name}" }
        updateModes.forEach { mode ->
            when (mode) {
                UpdateMode.API -> metadataUpdateMapper.toBookMetadataUpdate(metadata, seriesMeta, book)
                    .let { mediaServerClient.updateBookMetadata(book.id, it) }


                UpdateMode.COMIC_INFO -> {
                    if (book.deleted) return@forEach

                    val comicInfo =
                        if (writeSeriesMetadata) metadataUpdateMapper.toSeriesComicInfo(seriesMeta, metadata)
                        else metadataUpdateMapper.toComicInfo(metadata, seriesMeta)

                    comicInfo?.let { comicInfoWriter.writeMetadata(book.url, it) }
                }

//                UpdateMode.OPF -> {
//                    if (book.deleted) return@forEach
//
//                    if (writeSeriesMetadata) epubWriter.writeSeriesMetadata(Path.of(book.url), seriesMeta, metadata)
//                    else epubWriter.writeMetadata(Path.of(book.url), seriesMeta, metadata)
//                }
            }
        }

        val newThumbnail = if (uploadBookCovers) metadata?.thumbnail else null
        val thumbnailId = replaceBookThumbnail(book.id, newThumbnail)

        if (thumbnailId == null) {
            bookThumbnailsRepository.delete(book.id)
        } else {
            bookThumbnailsRepository.save(
                seriesId = book.seriesId,
                bookId = book.id,
                thumbnailId = thumbnailId,
            )
        }
    }

    private suspend fun replaceBookThumbnail(bookId: MediaServerBookId, thumbnail: Image?): MediaServerThumbnailId? {
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

    private suspend fun replaceSeriesThumbnail(
        seriesId: MediaServerSeriesId,
        thumbnail: Image?
    ): MediaServerThumbnailId? {
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

    private suspend fun resetSeriesMetadata(series: MediaServerSeries, removeComicInfo: Boolean) {
        mediaServerClient.resetSeriesMetadata(series.id, series.name)

        mediaServerClient.getBooks(series.id)
            .sortedWith(compareBy(natSortComparator) { it.name })
            .forEachIndexed { index, book ->
                if (removeComicInfo) comicInfoWriter.removeComicInfo(book.url)
                resetBookMetadata(book, index + 1)
            }

        replaceSeriesThumbnail(series.id, null)
        seriesThumbnailsRepository.delete(series.id)
    }

    private suspend fun resetBookMetadata(book: MediaServerBook, sortNumber: Int?) {
        mediaServerClient.resetBookMetadata(book.id, book.name, sortNumber)

        replaceBookThumbnail(book.id, null)
        bookThumbnailsRepository.delete(book.id)
    }

    private fun bookToWriteSeriesMetadata(bookMetadata: Map<MediaServerBook, BookMetadata?>): MediaServerBookId? {
        if (updateModes.none { it == UpdateMode.COMIC_INFO }) return null

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