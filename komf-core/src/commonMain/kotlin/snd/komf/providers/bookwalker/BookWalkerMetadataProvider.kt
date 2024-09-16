package snd.komf.providers.bookwalker

import snd.komf.providers.MetadataProvider
import snd.komf.util.NameSimilarityMatcher
import snd.komf.providers.CoreProviders
import snd.komf.providers.CoreProviders.BOOK_WALKER
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.MediaType
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.bookwalker.model.BookWalkerBook
import snd.komf.providers.bookwalker.model.BookWalkerBookId
import snd.komf.providers.bookwalker.model.BookWalkerCategory.LIGHT_NOVELS
import snd.komf.providers.bookwalker.model.BookWalkerCategory.MANGA
import snd.komf.providers.bookwalker.model.BookWalkerSearchResult
import snd.komf.providers.bookwalker.model.BookWalkerSeriesBook
import snd.komf.providers.bookwalker.model.BookWalkerSeriesId

class BookWalkerMetadataProvider(
    private val client: BookWalkerClient,
    private val metadataMapper: BookWalkerMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
    mediaType: MediaType,
) : MetadataProvider {
    private val category = when (mediaType) {
        MediaType.MANGA -> MANGA
        MediaType.NOVEL -> LIGHT_NOVELS
        MediaType.COMIC -> throw IllegalStateException("Comics media type is not supported")
    }

    override fun providerName(): CoreProviders = BOOK_WALKER

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val books = getAllBooks(BookWalkerSeriesId(seriesId.value))
        val firstBook = getFirstBook(books)
        val thumbnail = if (fetchSeriesCovers) getThumbnail(firstBook.imageUrl) else null
        return metadataMapper.toSeriesMetadata(BookWalkerSeriesId(seriesId.value), firstBook, books, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val books = getAllBooks(BookWalkerSeriesId(seriesId.value))
        val firstBook = getFirstBook(books)
        return getThumbnail(firstBook.imageUrl)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(BookWalkerBookId(bookId.id))
        val thumbnail = if (fetchBookCovers) getThumbnail(bookMetadata.imageUrl) else null

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(sanitizeSearchInput(seriesName.take(100)), category).take(limit)
        return searchResults.mapNotNull {
            getSeriesId(it)?.let { seriesId -> metadataMapper.toSeriesSearchResult(it, seriesId) }
        }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.searchSeries(sanitizeSearchInput(seriesName.take(100)), category)

        return searchResults
            .firstOrNull { nameMatcher.matches(seriesName, it.seriesName) }
            ?.let {
                getSeriesId(it)?.let { seriesId ->
                    val books = getAllBooks(seriesId)
                    val firstBook = getFirstBook(books)
                    val thumbnail = if (fetchSeriesCovers) getThumbnail(firstBook.imageUrl) else null
                    metadataMapper.toSeriesMetadata(seriesId, firstBook, books, thumbnail)
                }
            }
    }

    private suspend fun getSeriesId(searchResult: BookWalkerSearchResult): BookWalkerSeriesId? {
        return searchResult.seriesId ?: searchResult.bookId?.let { client.getBook(it).seriesId }
    }

    private suspend fun getThumbnail(url: String?): Image? = url?.let { client.getThumbnail(it) }

    private suspend fun getFirstBook(books: Collection<BookWalkerSeriesBook>): BookWalkerBook {
        val firstBook = books.sortedWith(compareBy(nullsLast()) { it.number?.start }).first()
        return client.getBook(firstBook.id)
    }

    private suspend fun getAllBooks(series: BookWalkerSeriesId): Collection<BookWalkerSeriesBook> {
        val books = mutableListOf<BookWalkerSeriesBook>()
        var pageNumber = 1
        var requestCount = 0
        do {
            val page = client.getSeriesBooks(series, pageNumber)
            books.addAll(page.books)
            pageNumber++
            requestCount++
        } while (page.page != page.totalPages && requestCount < 100)

        return books
    }

    private fun sanitizeSearchInput(name: String): String {
        return name
            .replace("[(]([^)]+)[)]".toRegex(), "")
            .trim()
    }
}
