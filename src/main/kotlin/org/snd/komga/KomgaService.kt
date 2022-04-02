package org.snd.komga

import mu.KotlinLogging
import org.snd.config.MetadataUpdateConfig
import org.snd.komga.model.MatchedBook
import org.snd.komga.model.MatchedSeries
import org.snd.komga.model.dto.AuthorUpdate
import org.snd.komga.model.dto.Book
import org.snd.komga.model.dto.BookId
import org.snd.komga.model.dto.BookMetadataUpdate
import org.snd.komga.model.dto.LibraryId
import org.snd.komga.model.dto.SeriesId
import org.snd.komga.model.dto.ThumbnailId
import org.snd.komga.model.dto.toSeriesUpdate
import org.snd.komga.repository.MatchedBookRepository
import org.snd.komga.repository.MatchedSeriesRepository
import org.snd.metadata.MetadataProvider
import org.snd.metadata.Provider
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.Thumbnail
import org.snd.metadata.model.VolumeMetadata
import org.snd.metadata.model.toBookMetadataUpdate

private val logger = KotlinLogging.logger {}

class KomgaService(
    private val komgaClient: KomgaClient,
    private val metadataProviders: Map<Provider, MetadataProvider>,
    private val matchedSeriesRepository: MatchedSeriesRepository,
    private val matchedBookRepository: MatchedBookRepository,
    private val metadataUpdateConfig: MetadataUpdateConfig
) {

    fun setSeriesMetadata(seriesId: SeriesId, providerName: Provider, providerSeriesId: ProviderSeriesId) {
        val provider = metadataProviders[providerName] ?: throw RuntimeException()
        val seriesMetadata = provider.getSeriesMetadata(providerSeriesId)

        updateSeriesMetadata(seriesId, seriesMetadata)
    }

    fun searchSeriesMetadata(seriesName: String): Collection<SeriesSearchResult> {
        return metadataProviders.values.flatMap { it.searchSeries(seriesName) }
    }

    fun matchSeriesMetadata(seriesId: SeriesId, provider: Provider? = null) {
        val series = komgaClient.getSeries(seriesId)
        if (provider != null) {
            val metadataProvider = metadataProviders[provider] ?: throw RuntimeException()
            metadataProvider.matchSeriesMetadata(series.name)?.let { meta -> updateSeriesMetadata(seriesId, meta) }
            return
        }

        metadataProviders.values
            .firstNotNullOfOrNull { it.matchSeriesMetadata(series.name) }
            ?.let { updateSeriesMetadata(seriesId, it) }
            ?: logger.info { "no match found for series ${series.name} ${series.id}" }
    }

    fun matchLibraryMetadata(libraryId: LibraryId, provider: Provider? = null) {
        var page = 0
        do {
            val currentPage = komgaClient.getSeries(libraryId, false, page)
            currentPage.content.forEach { matchSeriesMetadata(it.seriesId(), provider) }
            page++
        } while (!currentPage.last)
    }

    fun availableProviders(): Set<Provider> = metadataProviders.keys

    private fun updateSeriesMetadata(seriesId: SeriesId, metadata: SeriesMetadata) {
        logger.info { "updating $seriesId metadata to ${metadata.provider} ${metadata.id} ${metadata.title}" }
        komgaClient.updateSeriesMetadata(seriesId, metadata.toSeriesUpdate())
        updateBookMetadata(seriesId, metadata)

        val matchedSeries = matchedSeriesRepository.findFor(seriesId)
        val newThumbnail = if (!metadataUpdateConfig.seriesThumbnails) null else metadata.thumbnail
        val thumbnailId = replaceSeriesThumbnail(seriesId, newThumbnail, matchedSeries?.thumbnailId)

        val newMatch = MatchedSeries(
            seriesId = seriesId,
            thumbnailId = thumbnailId,
            provider = metadata.provider,
            providerSeriesId = metadata.id,
        )
        if (matchedSeries == null) matchedSeriesRepository.insert(newMatch)
        else matchedSeriesRepository.update(newMatch)
    }

    private fun updateBookMetadata(seriesId: SeriesId, seriesMeta: SeriesMetadata) {
        val books = komgaClient.getBooks(seriesId, true).content
        matchBooksToMedata(books, seriesMeta.volumeMetadata).forEach { (book, volumeMeta) ->
            if (volumeMeta != null) {
                updateBookMetadata(seriesId, book.bookId(), volumeMeta)

            } else {
                updateBookMetadata(seriesId, book.bookId(), seriesMeta)
            }
        }
    }

    private fun updateBookMetadata(seriesId: SeriesId, bookId: BookId, metadata: VolumeMetadata) {
        val matchedBook = matchedBookRepository.findFor(bookId)
        komgaClient.updateBookMetadata(bookId, metadata.toBookMetadataUpdate())
        val newThumbnail = if (!metadataUpdateConfig.bookThumbnails) null else metadata.thumbnail
        val thumbnailId = replaceBookThumbnail(bookId, newThumbnail, matchedBook?.thumbnailId)

        val newMatch = MatchedBook(
            seriesId = seriesId,
            bookId = bookId,
            thumbnailId = thumbnailId,
        )
        if (matchedBook == null) matchedBookRepository.insert(newMatch)
        else matchedBookRepository.update(newMatch)
    }

    private fun updateBookMetadata(seriesId: SeriesId, bookId: BookId, metadata: SeriesMetadata) {
        val matchedBook = matchedBookRepository.findFor(bookId)
        komgaClient.updateBookMetadata(
            bookId,
            BookMetadataUpdate(authors = metadata.authors?.map { author -> AuthorUpdate(author.name, author.role) })
        )
        replaceBookThumbnail(bookId, null, matchedBook?.thumbnailId)

        val newMatch = MatchedBook(
            seriesId = seriesId,
            bookId = bookId,
            thumbnailId = null,
        )
        if (matchedBook == null) matchedBookRepository.insert(newMatch)
        else matchedBookRepository.update(newMatch)
    }

    private fun replaceSeriesThumbnail(seriesId: SeriesId, thumbnail: Thumbnail?, oldThumbnail: ThumbnailId?): ThumbnailId? {
        val thumbnails = komgaClient.getSeriesThumbnails(seriesId)

        val thumbnailId = thumbnail?.let {
            komgaClient.uploadSeriesThumbnail(
                seriesId = seriesId,
                thumbnail = thumbnail,
                selected = thumbnails.isEmpty()
            )
        }

        oldThumbnail?.let { thumb ->
            if (thumbnails.any { it.id == thumb.id }) {
                komgaClient.deleteSeriesThumbnail(seriesId, thumb)
            }
        }

        return thumbnailId?.let { ThumbnailId(it.id) }
    }

    private fun replaceBookThumbnail(bookId: BookId, thumbnail: Thumbnail?, oldThumbnail: ThumbnailId?): ThumbnailId? {
        val thumbnails = komgaClient.getBookThumbnails(bookId)

        val thumbnailId = thumbnail?.let {
            komgaClient.uploadBookThumbnail(
                bookId = bookId,
                thumbnail = thumbnail,
                selected = thumbnails.all { it.type == "GENERATED" || it.id == oldThumbnail?.id }
            )
        }

        oldThumbnail?.let { thumb ->
            if (thumbnails.any { it.id == thumb.id }) {
                komgaClient.deleteBookThumbnail(bookId, thumb)
            }
        }

        return thumbnailId?.let { ThumbnailId(it.id) }
    }

    private fun matchBooksToMedata(books: Collection<Book>, metadata: Collection<VolumeMetadata>): Map<Book, VolumeMetadata?> {
//        val nameRegex = ("( - [c,d]?(?<startChapter>[0-9]*)-?[c,d]?(?<endChapter>[0-9]*)?)?" +
//                "(\\s?\\(?v(?<volume>[0-9]*)\\)?)?").toRegex()
        val nameRegex = ("(\\s?\\(?v(?<volume>[0-9]*)\\)?)").toRegex()

        return books.associateWith { book ->
            val matchedGroups = nameRegex.find(book.name)?.groups
//            val startChapter = matchedGroups?.get("startChapter")?.value?.toIntOrNull()
//            val endChapter = matchedGroups?.get("endChapter")?.value?.toIntOrNull()
            val volume = matchedGroups?.get("volume")?.value?.toIntOrNull()
            val meta = metadata.firstOrNull { meta -> meta.number != null && volume != null && meta.number == volume }

            meta
        }
    }
}
