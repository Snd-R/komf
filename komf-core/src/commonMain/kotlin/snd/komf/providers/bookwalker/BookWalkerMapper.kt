package snd.komf.providers.bookwalker

import kotlinx.datetime.TimeZone.Companion.UTC
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.BookMetadata
import snd.komf.model.BookRange
import snd.komf.model.Image
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.ReadingDirection
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesBook
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType.LOCALIZED
import snd.komf.model.WebLink
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders.BOOK_WALKER
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.bookwalker.model.BookWalkerBook
import snd.komf.providers.bookwalker.model.BookWalkerContentType
import snd.komf.providers.bookwalker.model.BookWalkerContributor
import snd.komf.providers.bookwalker.model.BookWalkerContributorRole
import snd.komf.providers.bookwalker.model.BookWalkerSeries

const val bookWalkerBaseUrl = "https://bookwalker.com"
const val sosBrigadeCDN = "https://img.sos-dan.net"

class BookWalkerMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(
        series: BookWalkerSeries,
        books: List<BookWalkerBook>,
        thumbnail: Image?
    ): ProviderSeriesMetadata {
        val titles = listOf(SeriesTitle(series.title, LOCALIZED, "en"))
            .plus(series.altTitles.map { SeriesTitle(it, null, null) })

        val listedDate = series.listedAt.toLocalDateTime(UTC)

        val metadata = SeriesMetadata(
            titles = titles,
            summary = series.description,
            //TODO, no relations between series/book in database
            publisher = null,
            tags = series.tags.map { it.name },
            readingDirection = when (series.type) {
                BookWalkerContentType.MANGA -> ReadingDirection.RIGHT_TO_LEFT
                BookWalkerContentType.WEBTOONS -> ReadingDirection.WEBTOON
                BookWalkerContentType.NOVEL,
                BookWalkerContentType.AUDIOBOOK -> null
            },
            totalBookCount = books.size,
            authors = getAuthors(books.flatMap { it.contributors }.distinct()),
            releaseDate = ReleaseDate(
                year = listedDate.year,
                month = listedDate.month.number,
                day = listedDate.day,
            ),
            links = listOf(WebLink("BookWalker", series.url)),
            thumbnail = thumbnail,
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(series.id.value),
            metadata = metadata,
            books = books.map {
                SeriesBook(
                    id = ProviderBookId(it.id.value),
                    number = BookRange(it.displayOrder),
                    name = it.displayTitle,
                    type = null,
                    edition = null
                )
            }
        )

        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(book: BookWalkerBook, thumbnail: Image?): ProviderBookMetadata {
        val metadata = BookMetadata(
            title = book.title,
            summary = book.description,
            number = BookRange(book.displayOrder),
            releaseDate = book.onSaleAt.toLocalDateTime(UTC).date,
            authors = getAuthors(book.contributors),
//            tags = book.tags.map { it.name }.toSet(),
            startChapter = null,
            endChapter = null,
            isbn = book.isbn,
            thumbnail = thumbnail,
            links = listOf(WebLink("BookWalker", book.url))
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.value),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun getAuthors(contributors: List<BookWalkerContributor>): List<Author> {
        return contributors.mapNotNull {
            when (it.role) {
                BookWalkerContributorRole.CONTRIBUTOR -> null
                BookWalkerContributorRole.AUTHOR -> authorRoles.map { role -> Author(it.name, role) }
                BookWalkerContributorRole.ARTIST -> artistRoles.map { role -> Author(it.name, role) }
                BookWalkerContributorRole.ILLUSTRATOR -> listOf(Author(it.name, AuthorRole.PENCILLER))
                BookWalkerContributorRole.UNKNOWN_4 -> null
                BookWalkerContributorRole.UNKNOWN_5 -> null
                BookWalkerContributorRole.COLORIST -> listOf(Author(it.name, AuthorRole.COLORIST))
                BookWalkerContributorRole.LETTERER -> listOf(Author(it.name, AuthorRole.LETTERER))
                BookWalkerContributorRole.EDITOR -> listOf(Author(it.name, AuthorRole.EDITOR))
                BookWalkerContributorRole.TRANSLATOR -> listOf(Author(it.name, AuthorRole.TRANSLATOR))
                BookWalkerContributorRole.NARRATOR -> null
                BookWalkerContributorRole.ORIGINAL_AUTHOR -> listOf(Author(it.name, AuthorRole.WRITER))
                BookWalkerContributorRole.ORIGINAL_CHARACTER_DESIGN -> null
                BookWalkerContributorRole.ADAPTATION -> listOf(Author(it.name, AuthorRole.WRITER))
                BookWalkerContributorRole.COMPILATION -> null
                BookWalkerContributorRole.CONSULTING_EDITOR -> listOf(Author(it.name, AuthorRole.EDITOR))
                BookWalkerContributorRole.COVER_DESIGN -> listOf(Author(it.name, AuthorRole.COVER))
                BookWalkerContributorRole.CREATOR -> listOf(Author(it.name, AuthorRole.WRITER))
                BookWalkerContributorRole.DESIGNER -> listOf(Author(it.name, AuthorRole.PENCILLER))
                BookWalkerContributorRole.COORDINATION -> null
                BookWalkerContributorRole.IDEA -> null
                BookWalkerContributorRole.UNKNOWN_21 -> null
                BookWalkerContributorRole.PRODUCER -> null
                BookWalkerContributorRole.SCRIPT -> listOf(Author(it.name, AuthorRole.WRITER))
                BookWalkerContributorRole.TEXT -> listOf(Author(it.name, AuthorRole.WRITER))
                BookWalkerContributorRole.WITH -> null
                BookWalkerContributorRole.UNKNOWN -> null
            }
        }.flatten()
    }

    fun toSeriesSearchResult(series: BookWalkerSeries): SeriesSearchResult {
        return SeriesSearchResult(
            url = series.url,
            imageUrl = series.image?.url600,
            title = series.title,
            provider = BOOK_WALKER,
            resultId = series.id.value
        )
    }
}
