package snd.komf.providers.kodansha

import com.fleeksoft.ksoup.Ksoup
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.BookMetadata
import snd.komf.model.BookRange
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
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType
import snd.komf.model.WebLink
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.kodansha.model.KodanshaBook
import snd.komf.providers.kodansha.model.KodanshaSearchResult
import snd.komf.providers.kodansha.model.KodanshaSeries

const val kodanshaBaseUrl = "https://kodansha.us"

class KodanshaMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {

    fun toSeriesMetadata(
        series: KodanshaSeries,
        bookList: List<KodanshaBook>,
        thumbnail: Image? = null
    ): ProviderSeriesMetadata {
        val status = when (series.completionStatus) {
            "Ongoing" -> SeriesStatus.ONGOING
            "Completed" -> SeriesStatus.ENDED
            "Complete" -> SeriesStatus.ENDED
            else -> null
        }
        val seriesTitle = series.title.removeSuffix("(manga)")
        val ageRating = series.ageRating?.removeSuffix("+")?.toIntOrNull()

        val author = if (series.creators?.size == 1) Author(series.creators.first().name, AuthorRole.WRITER) else null
        val metadata = SeriesMetadata(
            status = status,
            titles = listOf(SeriesTitle(seriesTitle, TitleType.LOCALIZED, "en")),
            summary = series.description?.let { parseDescription(it) },
            publisher = series.publisher?.let { Publisher(it, PublisherType.LOCALIZED) },
            ageRating = ageRating,
            genres = series.genres?.map { it.name } ?: emptyList(),
            totalBookCount = if (bookList.isEmpty()) null else bookList.size,
            thumbnail = thumbnail,
            authors = author?.let { listOf(it) } ?: emptyList(),
            links = listOf(WebLink("Kodansha", seriesUrl(series.readableUrl ?: series.id.toString())))
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(series.id.toString()),
            metadata = metadata,
            books = bookList.map {
                SeriesBook(
                    id = ProviderBookId(it.id.toString()),
                    number = it.volumeNumber?.let { volumeNumber ->
                        if (volumeNumber == 0) null
                        else BookRange(volumeNumber.toDouble())
                    },
                    name = "${series.title} ${it.volumeNumber}",
                    type = null,
                    edition = null
                )
            }
        )
        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(book: KodanshaBook, thumbnail: Image? = null): ProviderBookMetadata {
        val author = if (book.creators?.size == 1) Author(book.creators.first().name, AuthorRole.WRITER) else null
        val metadata = BookMetadata(
            title = book.name,
            summary = book.description?.let { parseDescription(it) },
            number = book.volumeNumber?.let { volumeNumber ->
                if (volumeNumber == 0) null
                else BookRange(volumeNumber.toDouble())
            },
            releaseDate = book.readable.digitalReleaseDate?.date ?: book.readable.printReleaseDate?.date,
            isbn = book.readable.eisbn ?: book.readable.isbn,
            authors = author?.let { listOf(it) } ?: emptyList(),
            thumbnail = thumbnail,
            links = listOf(WebLink("Kodansha", bookUrl(book.readableUrl ?: book.id.toString())))
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.toString()),
            metadata = metadata
        )

        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    fun toSeriesSearchResult(result: KodanshaSearchResult): SeriesSearchResult {
        return SeriesSearchResult(
            url = bookUrl(result.content.readableUrl ?: result.content.id.toString()),
            imageUrl = result.content.thumbnails.firstOrNull()?.url,
            title = result.content.title,
            resultId = result.content.id.toString(),
            provider = CoreProviders.KODANSHA,
        )
    }

    private fun parseDescription(description: String): String {
        return Ksoup.parse(description).wholeText().replace("\n\n", "\n")
    }

    private fun seriesUrl(bookUrlPath: String) = "${kodanshaBaseUrl}/series/${bookUrlPath}"
    private fun bookUrl(productUrlPath: String) = "${kodanshaBaseUrl}/product/${productUrlPath}"

}
