package snd.komf.providers.yenpress

import io.ktor.http.*
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
import snd.komf.model.SeriesBook
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType.LOCALIZED
import snd.komf.model.WebLink
import snd.komf.model.toReleaseDate
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.yenpress.model.YenPressAuthor
import snd.komf.providers.yenpress.model.YenPressBook
import snd.komf.providers.yenpress.model.YenPressBookId
import snd.komf.providers.yenpress.model.YenPressBookShort
import snd.komf.providers.yenpress.model.YenPressSearchResult
import snd.komf.providers.yenpress.model.YenPressSeriesId

class YenPressMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    fun toSeriesMetadata(
        book: YenPressBook,
        books: List<YenPressBookShort>,
        thumbnail: Image? = null
    ): ProviderSeriesMetadata {

        val metadata = SeriesMetadata(
            status = null,
            titles = listOf(
                SeriesTitle(
                    bookTitle(book.name),
                    LOCALIZED,
                    "en"
                )
            ),
            authors = authors(book.authors),
            summary = book.description,
            genres = book.genres,
            tags = emptyList(),
            releaseDate = book.releaseDate?.toReleaseDate(),
            ageRating = book.ageRating?.let { ageRating(it) },
            publisher = book.imprint?.let { Publisher(it, PublisherType.LOCALIZED) },
            thumbnail = thumbnail,
            totalBookCount = books.ifEmpty { null }?.size,
            links = listOf(
                WebLink(
                    "YenPress",
                    "${yenPressBaseUrl}series/${book.seriesId.value.encodeURLPathPart()}"
                )
            )
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(book.id.value),
            metadata = metadata,
            books = books.map {
                SeriesBook(
                    id = ProviderBookId(it.id.value),
                    number = it.number,
                    name = it.name,
                    type = null,
                    edition = null
                )
            }
        )

        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(book: YenPressBook, thumbnail: Image? = null): ProviderBookMetadata {
        val metadata = BookMetadata(
            number = book.number,
            title = book.name,
            authors = authors(book.authors),
            summary = book.description,
            releaseDate = book.releaseDate,
            isbn = book.isbn,
            startChapter = null,
            endChapter = null,
            thumbnail = thumbnail,
            links = listOf(WebLink("YenPress", bookUrl(book.id)))
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.value),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun authors(authors: List<YenPressAuthor>): List<Author> {
        return authors.flatMap { (role, name) ->
            when (role) {
                "Author", "Original author" -> authorRoles.map { Author(role = it, name = name) }
                "Illustrated by:", "Artist" -> artistRoles.map { Author(role = it, name = name) }
                "Created by" -> (authorRoles + artistRoles).map { Author(role = it, name = name) }
                "Translated by" -> listOf(Author(role = AuthorRole.TRANSLATOR, name = name))
                "Letterer" -> listOf(Author(role = AuthorRole.LETTERER, name = name))
                else -> emptyList()
            }
        }
    }

    private fun ageRating(ageRating: String): Int? {
        return when (ageRating) {
            "All Ages" -> 6
            "T (Teen)" -> 13
            "OT (Older Teen)" -> 16
            "18+ M (Mature)" -> 18
            else -> null
        }
    }

    fun toSeriesSearchResult(result: YenPressSearchResult): SeriesSearchResult {
        return SeriesSearchResult(
            url = seriesUrl(result.id),
            provider = CoreProviders.YEN_PRESS,
            title = result.title.raw,
            resultId = result.id.value,
            imageUrl = result.image?.raw,
        )
    }

    private fun seriesUrl(id: YenPressSeriesId) = "${yenPressBaseUrl}series/${id.value.encodeURLPathPart()}"
    private fun bookUrl(id: YenPressBookId) = "$yenPressBaseUrl/titles/${id.value}"
}

fun bookTitle(name: String) = name
    .replace("(\\(light novel\\))|(\\(manga\\))".toRegex(), "")
    .replace(", Vol. [0-9]+".toRegex(), "")
    .trim()
