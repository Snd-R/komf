package snd.komf.providers.viz

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
import snd.komf.model.SeriesStatus.ENDED
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType.LOCALIZED
import snd.komf.model.WebLink
import snd.komf.model.toReleaseDate
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.viz.model.VizAllBooksId
import snd.komf.providers.viz.model.VizBook
import snd.komf.providers.viz.model.VizBookId
import snd.komf.providers.viz.model.VizSeriesBook

class VizMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(
        book: VizBook,
        allBooks: Collection<VizSeriesBook>,
        thumbnail: Image? = null
    ): ProviderSeriesMetadata {
        val metadata = SeriesMetadata(
            status = if (allBooks.any { it.final }) ENDED else null,
            titles = listOf(SeriesTitle(book.seriesName, LOCALIZED, "en")),
            summary = book.description,
            publisher = Publisher(book.publisher, PublisherType.LOCALIZED),
            ageRating = book.ageRating?.age,
            genres = book.genres,
            tags = emptyList(),
            totalBookCount = allBooks.size.let { if (it < 1) null else it },
            authors = getAuthors(book),
            thumbnail = thumbnail,
            releaseDate = book.releaseDate?.toReleaseDate(),
            links = book.allBooksId
                ?.let { listOf(WebLink("Viz", seriesUrl(book.allBooksId))) }
                ?: emptyList()
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(book.id.value),
            metadata = metadata,
            books = allBooks.map {
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

    fun toBookMetadata(book: VizBook, thumbnail: Image? = null): ProviderBookMetadata {
        val metadata = BookMetadata(
            title = book.name,
            summary = book.description,
            number = book.number,
            releaseDate = book.releaseDate,
            authors = getAuthors(book),
            isbn = book.isbn,
            startChapter = null,
            endChapter = null,
            thumbnail = thumbnail,
            links = listOf(WebLink("Viz", bookUrl(book.id)))
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.value),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    fun toSeriesSearchResult(book: VizSeriesBook): SeriesSearchResult {
        return SeriesSearchResult(
            url = null,
            imageUrl = book.imageUrl,
            title = book.name,
            provider = CoreProviders.VIZ,
            resultId = book.id.value
        )
    }

    private fun seriesUrl(id: VizAllBooksId) = "$vizBaseUrl/manga-books/manga/${id.id.encodeURLPath()}/all"
    private fun bookUrl(id: VizBookId) = "$vizBaseUrl/manga-books/manga/${id.value}"

    private fun getAuthors(book: VizBook): List<Author> {
        val authorsArt = book.authorArt?.let { name ->
            artistRoles.map { role -> Author(name, role) }
        } ?: emptyList()
        val authorStory = book.authorStory?.let { name ->
            authorRoles.map { role -> Author(name, role) }
        } ?: emptyList()
        return authorsArt + authorStory
    }
}
