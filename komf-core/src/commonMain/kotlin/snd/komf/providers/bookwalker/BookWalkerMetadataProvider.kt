package snd.komf.providers.bookwalker

import io.github.reactivecircus.cache4k.Cache
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.MediaType
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.CoreProviders.BOOK_WALKER
import snd.komf.providers.MetadataProvider
import snd.komf.providers.bookwalker.model.BookWalkerBook
import snd.komf.providers.bookwalker.model.BookWalkerBookId
import snd.komf.providers.bookwalker.model.BookWalkerBookInfo
import snd.komf.providers.bookwalker.model.BookWalkerCategory.LIGHT_NOVELS
import snd.komf.providers.bookwalker.model.BookWalkerCategory.MANGA
import snd.komf.providers.bookwalker.model.BookWalkerSearchResult
import snd.komf.providers.bookwalker.model.BookWalkerSeriesBook
import snd.komf.providers.bookwalker.model.BookWalkerSeriesId
import snd.komf.util.NameSimilarityMatcher
import kotlin.time.Duration.Companion.minutes

private const val restrictedCover = "https://rimg.bookwalker.jp/599999999"

class BookWalkerMetadataProvider(
    private val client: BookWalkerClient,
    private val metadataMapper: BookWalkerMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
    mediaType: MediaType,
) : MetadataProvider {
    private val category = when (mediaType) {
        // Webtoon "V-Scroll" is a genre inside MANGA https://global.bookwalker.jp/genre/7411/
        MediaType.MANGA, MediaType.WEBTOON -> MANGA
        MediaType.NOVEL -> LIGHT_NOVELS
        MediaType.COMIC -> throw IllegalStateException("Comics media type is not supported")
    }

    private val seriesCache = Cache.Builder<BookWalkerSeriesId, Collection<BookWalkerSeriesBook>>()
        .expireAfterWrite(30.minutes)
        .build()

    private val bookCache = Cache.Builder<BookWalkerBookId, BookWalkerBook>()
        .expireAfterWrite(30.minutes)
        .build()
    private val bookInfoCache = Cache.Builder<BookWalkerBookId, BookWalkerBookInfo>()
        .expireAfterWrite(30.minutes)
        .build()


    override fun providerName(): CoreProviders = BOOK_WALKER

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val books = getAllBooks(BookWalkerSeriesId(seriesId.value))
        val firstBook = getFirstBook(books)
        val cover = if (fetchSeriesCovers) fetchCover(firstBook) else null
        return metadataMapper.toSeriesMetadata(BookWalkerSeriesId(seriesId.value), firstBook, books, cover)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val books = getAllBooks(BookWalkerSeriesId(seriesId.value))
        return fetchCover(getFirstBook(books))
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = bookCache.get(BookWalkerBookId(bookId.id)) { client.getBook(BookWalkerBookId(bookId.id)) }
        val bookCover = if (fetchBookCovers) fetchCover(bookMetadata) else null
        return metadataMapper.toBookMetadata(bookMetadata, bookCover)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(sanitizeSearchInput(seriesName.take(100)), category).take(limit)
        val res = searchResults.map { processSearchResult(it) }
            .mapNotNull { result ->
                getSeriesId(result)?.let { metadataMapper.toSeriesSearchResult(result, it) }
            }
        return res
    }

    private suspend fun processSearchResult(result: BookWalkerSearchResult): BookWalkerSearchResult {
        if (result.imageUrl == null || !result.imageUrl.startsWith(restrictedCover)) return result

        val newImageUrl = when {
            result.seriesId != null -> {
                val firstBook = getFirstBook(getAllBooks(result.seriesId))
                getCoverUrlFromApi(firstBook.id)
            }

            result.bookId != null -> getCoverUrlFromApi(result.bookId)
            else -> null
        }

        return result.copy(imageUrl = newImageUrl)
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
                    val bookCover = if (fetchSeriesCovers) fetchCover(firstBook) else null
                    metadataMapper.toSeriesMetadata(seriesId, firstBook, books, bookCover)
                }
            }
    }

    private suspend fun getSeriesId(searchResult: BookWalkerSearchResult): BookWalkerSeriesId? {
        return searchResult.seriesId ?: searchResult.bookId?.let { bookCache.get(it) { client.getBook(it) }.seriesId }
    }

    private suspend fun getFirstBook(books: Collection<BookWalkerSeriesBook>): BookWalkerBook {
        val firstBook = books.sortedWith(compareBy(nullsLast()) { it.number?.start }).first()
        return bookCache.get(firstBook.id) { client.getBook(firstBook.id) }
    }

    private suspend fun getAllBooks(series: BookWalkerSeriesId): Collection<BookWalkerSeriesBook> {
        return seriesCache.get(series) {
            val books = mutableListOf<BookWalkerSeriesBook>()
            var pageNumber = 1
            var requestCount = 0
            do {
                val page = client.getSeriesBooks(series, pageNumber)
                books.addAll(page.books)
                pageNumber++
                requestCount++
            } while (page.page != page.totalPages && requestCount < 100)

            books
        }
    }

    private fun sanitizeSearchInput(name: String): String {
        return name
            .replace("[(]([^)]+)[)]".toRegex(), "")
            .trim()
    }

    private suspend fun fetchCover(book: BookWalkerBook): Image? {
        if (book.imageUrl == null || book.imageUrl.startsWith(restrictedCover)) {
            val url = getCoverUrlFromApi(book.id)
            return client.getThumbnail(url)
        } else {
            return client.getThumbnail(book.imageUrl)
        }
    }

    private suspend fun getCoverUrlFromApi(bookId: BookWalkerBookId): String {
        return bookInfoCache.get(bookId) { client.getBookApi(bookId) }.thumbnailImageUrl
    }
}
