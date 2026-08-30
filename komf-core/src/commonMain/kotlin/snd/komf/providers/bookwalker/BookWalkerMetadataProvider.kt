package snd.komf.providers.bookwalker

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.http.contentType
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
import snd.komf.providers.bookwalker.model.BookWalkerBookId
import snd.komf.providers.bookwalker.model.BookWalkerContentType
import snd.komf.providers.bookwalker.model.BookWalkerImage
import snd.komf.providers.bookwalker.model.BookWalkerSeriesId
import snd.komf.providers.bookwalker.db.BookWalkerSeriesRepository
import snd.komf.util.NameSimilarityMatcher

class BookWalkerMetadataProvider(
    private val metadataMapper: BookWalkerMapper,
    private val repository: BookWalkerSeriesRepository,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
    private val httpClient: HttpClient?,
    mediaType: MediaType,
) : MetadataProvider {
    private val category = when (mediaType) {
        MediaType.MANGA -> BookWalkerContentType.MANGA
        MediaType.WEBTOON -> BookWalkerContentType.WEBTOONS
        MediaType.NOVEL -> BookWalkerContentType.NOVEL
        MediaType.COMIC -> throw IllegalStateException("Comics media type is not supported")
    }

    override fun providerName(): CoreProviders = BOOK_WALKER

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val bookWalkerId = BookWalkerSeriesId(seriesId.value)
        val series = repository.getSeries(bookWalkerId)
        val books = repository.getSeriesBooks(bookWalkerId)
        val cover = if (fetchSeriesCovers) series.image?.let { fetchCover(it) } else null
        return metadataMapper.toSeriesMetadata(series, books, cover)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = repository.getSeries(BookWalkerSeriesId(seriesId.value))
        return series.image?.let { fetchCover(it) }
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val book = repository.getBook(BookWalkerBookId(bookId.id))
        val cover = if (fetchBookCovers) book.image?.let { fetchCover(it) } else null
        return metadataMapper.toBookMetadata(book, cover)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val results = repository.search(seriesName, listOf(category))
        return results.map { metadataMapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = repository.search(seriesName, listOf(category))
        return searchResults
            .firstOrNull { nameMatcher.matches(seriesName, it.title + it.altTitles) }
            ?.let { series ->
                val books = repository.getSeriesBooks(series.id)
                val cover = if (fetchSeriesCovers) series.image?.let { fetchCover(it) } else null
                metadataMapper.toSeriesMetadata(series, books, cover)
            }
    }

    private suspend fun fetchCover(image: BookWalkerImage): Image? {
        if (httpClient == null) return null
        try {
            val response = httpClient.get(image.url600)
            return Image(
                response.body(),
                response.contentType()?.let { "${it.contentType}/${it.contentSubtype}" }
            )
        } catch (_: ResponseException) {
            return null
        }

    }
}
