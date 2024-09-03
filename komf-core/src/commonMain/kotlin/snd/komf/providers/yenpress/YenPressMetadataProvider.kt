package snd.komf.providers.yenpress

import snd.komf.model.Image
import snd.komf.providers.MetadataProvider
import snd.komf.util.NameSimilarityMatcher
import snd.komf.providers.CoreProviders
import snd.komf.providers.CoreProviders.YEN_PRESS
import snd.komf.model.MatchQuery
import snd.komf.model.MediaType
import snd.komf.model.MediaType.COMIC
import snd.komf.model.MediaType.MANGA
import snd.komf.model.MediaType.NOVEL
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.yenpress.model.YenPressBookId
import snd.komf.providers.yenpress.model.YenPressSeriesId

class YenPressMetadataProvider(
    private val client: YenPressClient,
    private val metadataMapper: YenPressMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val mediaType: MediaType,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {
    init {
        if (mediaType == COMIC) throw IllegalStateException("Comics media type is not supported")
    }

    override fun providerName(): CoreProviders {
        return YEN_PRESS
    }

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val allBooks = client.getBookList(YenPressSeriesId(seriesId.value))
        val firstBook = allBooks.firstOrNull { it.number != null }
            ?: (if (allBooks.size == 1) allBooks.first() else null)
            ?: throw IllegalStateException("Can't find first book")

        val seriesBook = client.getBook(firstBook.id)
        val thumbnail = if (fetchSeriesCovers) client.getBookThumbnail(seriesBook) else null

        return metadataMapper.toSeriesMetadata(seriesBook, allBooks, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val allBooks = client.getBookList(YenPressSeriesId(seriesId.value))
        return allBooks.firstOrNull()?.let { first->
            val book = client.getBook(first.id)
            client.getBookThumbnail(book)
        }
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(YenPressBookId(bookId.id))
        val thumbnail = if (fetchBookCovers) client.getBookThumbnail(bookMetadata) else null

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(128)).take(limit)
        return searchResults.map { metadataMapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.searchSeries(seriesName.take(128))

        return searchResults
            .filter { !it.title.raw.contains("(audio)") }
            .filter {
                when (mediaType) {
                    MANGA -> !it.title.raw.contains("(light novel)")
                    NOVEL -> !it.title.raw.contains("(manga)")
                    COMIC -> false
                }
            }
            .firstOrNull { nameMatcher.matches(seriesName, seriesTitleFromBook(it.title.raw)) }
            ?.let { getSeriesMetadata(it.id) }
    }

    private suspend fun getSeriesMetadata(seriesId: YenPressSeriesId): ProviderSeriesMetadata? {
        val books = client.getBookList(seriesId)
        val firstBook = books.find { book -> book.number?.start != null }
            ?: (if (books.size == 1) books.first() else null)

        val seriesBook = firstBook?.let { client.getBook(it.id) } ?: return null
        val thumbnail = if (fetchSeriesCovers) client.getBookThumbnail(seriesBook) else null
        return metadataMapper.toSeriesMetadata(seriesBook, books, thumbnail)
    }
}
