package snd.komf.providers.bookwalker

import io.ktor.http.encodeURLPathPart
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.BookMetadata
import snd.komf.model.Image
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.Publisher
import snd.komf.model.PublisherType
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesBook
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType.LOCALIZED
import snd.komf.model.TitleType.NATIVE
import snd.komf.model.TitleType.ROMAJI
import snd.komf.model.WebLink
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders.BOOK_WALKER
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.bookwalker.model.BookWalkerBook
import snd.komf.providers.bookwalker.model.BookWalkerBookId
import snd.komf.providers.bookwalker.model.BookWalkerSearchResult
import snd.komf.providers.bookwalker.model.BookWalkerSeriesBook
import snd.komf.providers.bookwalker.model.BookWalkerSeriesId

class BookWalkerMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(
        seriesId: BookWalkerSeriesId,
        book: BookWalkerBook,
        allBooks: Collection<BookWalkerSeriesBook>,
        thumbnail: Image? = null
    ): ProviderSeriesMetadata {
        val titles = listOfNotNull(
            book.seriesTitle?.let { SeriesTitle(it, LOCALIZED, "en") },
            book.romajiTitle?.let { SeriesTitle(it, ROMAJI, "ja-ro") },
            book.japaneseTitle?.let { SeriesTitle(it, NATIVE, "ja") }
        )

        val metadata = SeriesMetadata(
            titles = titles,
            summary = book.synopsis,
            publisher = Publisher(book.publisher, PublisherType.LOCALIZED),
            genres = book.genres,
            tags = emptyList(),
            totalBookCount = allBooks.size.let { if (it < 1) null else it },
            authors = getAuthors(book),
            thumbnail = thumbnail,
            releaseDate = ReleaseDate(
                year = book.availableSince?.year,
                month = book.availableSince?.monthNumber,
                day = book.availableSince?.dayOfMonth,
            ),
            links = listOf(WebLink("BookWalker", seriesUrl(seriesId)))
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(seriesId.id),
            metadata = metadata,
            books = allBooks.map {
                SeriesBook(
                    id = ProviderBookId(it.id.id),
                    number = it.number,
                    name = it.name,
                    type = null,
                    edition = null
                )
            }
        )

        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(book: BookWalkerBook, thumbnail: Image? = null): ProviderBookMetadata {
        val metadata = BookMetadata(
            title = book.name,
            summary = book.synopsis,
            number = book.number,
            releaseDate = book.availableSince,
            authors = getAuthors(book),
            startChapter = null,
            endChapter = null,
            thumbnail = thumbnail,
            links = listOf(WebLink("BookWalker", bookUrl(book.id)))
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.id),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun getAuthors(book: BookWalkerBook): List<Author> {
        val artists = book.artists.flatMap { name -> artistRoles.map { role -> Author(name, role) } }
        val authors = book.authors.flatMap { name -> authorRoles.map { role -> Author(name, role) } }
        return artists + authors
    }

    fun toSeriesSearchResult(result: BookWalkerSearchResult, seriesId: BookWalkerSeriesId): SeriesSearchResult {
        return SeriesSearchResult(
            url = seriesUrl(seriesId),
            imageUrl = result.imageUrl,
            title = result.seriesName,
            provider = BOOK_WALKER,
            resultId = seriesId.id
        )
    }

    private fun seriesUrl(seriesId: BookWalkerSeriesId) =
        bookWalkerBaseUrl + "/series/${seriesId.id.encodeURLPathPart()}"

    private fun bookUrl(bookId: BookWalkerBookId) = bookWalkerBaseUrl + "/${bookId.id.encodeURLPathPart()}"
}
