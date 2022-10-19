package org.snd.metadata.providers.bookwalker

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Image
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.BOOK_WALKER
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesMatchResult
import org.snd.metadata.model.SeriesMatchStatus
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.providers.bookwalker.model.BookWalkerBook
import org.snd.metadata.providers.bookwalker.model.BookWalkerBookId
import org.snd.metadata.providers.bookwalker.model.BookWalkerSeriesBook
import org.snd.metadata.providers.bookwalker.model.BookWalkerSeriesId
import org.snd.metadata.providers.bookwalker.model.toSeriesSearchResult

class BookWalkerMetadataProvider(
    private val client: BookWalkerClient,
    private val metadataMapper: BookWalkerMapper,
    private val nameMatcher: NameSimilarityMatcher,
) : MetadataProvider {
    override fun providerName(): Provider = BOOK_WALKER

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val books = client.getSeriesBooks(BookWalkerSeriesId(seriesId.id))
        val firstBook = getFirstBook(books)
        val thumbnail = getThumbnail(firstBook.imageUrl)
        return metadataMapper.toSeriesMetadata(BookWalkerSeriesId(seriesId.id), firstBook, books, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(BookWalkerBookId(bookId.id))
        val thumbnail = getThumbnail(bookMetadata.imageUrl)

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(100)).take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): SeriesMatchResult {
        val searchResults = client.searchSeries(seriesName.take(100))

        val metadata = searchResults
            .firstOrNull { nameMatcher.matches(seriesName, it.seriesName) }
            ?.let {
                val books = client.getSeriesBooks(it.id)
                val firstBook = getFirstBook(books)
                val thumbnail = getThumbnail(firstBook.imageUrl)
                metadataMapper.toSeriesMetadata(it.id, firstBook, books, thumbnail)
            }

        return SeriesMatchResult(
            status = if (metadata == null) SeriesMatchStatus.NO_MATCH else SeriesMatchStatus.MATCHED,
            result = metadata
        )
    }

    private fun getThumbnail(url: String?): Image? = url?.toHttpUrl()?.let { client.getThumbnail(it) }

    private fun getFirstBook(books: Collection<BookWalkerSeriesBook>): BookWalkerBook {
        val firstBook = books.sortedWith(compareBy(nullsLast()) { it.number }).first()
        return client.getBook(firstBook.id)
    }
}
