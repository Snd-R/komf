package org.snd.metadata.providers.yenpress

import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.MatchQuery
import org.snd.metadata.model.MediaType
import org.snd.metadata.model.MediaType.MANGA
import org.snd.metadata.model.MediaType.NOVEL
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.YEN_PRESS
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.yenpress.model.YenPressBookId
import org.snd.metadata.providers.yenpress.model.YenPressSeriesId
import org.snd.metadata.providers.yenpress.model.toSeriesSearchResult

class YenPressMetadataProvider(
    private val client: YenPressClient,
    private val metadataMapper: YenPressMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val mediaType: MediaType,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {

    override fun providerName(): Provider {
        return YEN_PRESS
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val allBooks = client.getBookList(YenPressSeriesId(seriesId.id))
        val firstBook = allBooks.firstOrNull { it.number != null }
            ?: (if (allBooks.size == 1) allBooks.first() else null)
            ?: throw IllegalStateException("Can't find first book")

        val seriesBook = client.getBook(firstBook.id)
        val thumbnail = if (fetchSeriesCovers) client.getBookThumbnail(seriesBook) else null

        return metadataMapper.toSeriesMetadata(seriesBook, allBooks, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(YenPressBookId(bookId.id))
        val thumbnail = if (fetchBookCovers) client.getBookThumbnail(bookMetadata) else null

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(128)).take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.searchSeries(seriesName.take(128))

        return searchResults
            .filter { !it.title.contains("(audio)") }
            .filter {
                when (mediaType) {
                    MANGA -> !it.title.contains("(light novel)")
                    NOVEL -> !it.title.contains("(manga)")
                }
            }
            .firstOrNull { nameMatcher.matches(seriesName, bookTitle(it.title)) }
            ?.let { getSeriesMetadata(it.id) }
    }

    private fun getSeriesMetadata(seriesId: YenPressSeriesId): ProviderSeriesMetadata? {
        val books = client.getBookList(seriesId)
        val firstBook = books.find { book -> book.number?.start != null }
            ?: (if (books.size == 1) books.first() else null)

        val seriesBook = firstBook?.let { client.getBook(it.id) } ?: return null
        val thumbnail = if (fetchSeriesCovers) client.getBookThumbnail(seriesBook) else null
        return metadataMapper.toSeriesMetadata(seriesBook, books, thumbnail)
    }
}
