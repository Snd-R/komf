package org.snd.metadata.providers.kodansha

import org.jsoup.Jsoup
import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.metadata.Author
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.BookRange
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.SeriesBook
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesStatus
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.TitleType
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.providers.kodansha.model.KodanshaBook
import org.snd.metadata.providers.kodansha.model.KodanshaSeries

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
            publisher = series.publisher,
            ageRating = ageRating,
            genres = series.genres?.map { it.name } ?: emptyList(),
            totalBookCount = if (bookList.isEmpty()) null else bookList.size,
            thumbnail = thumbnail,
            authors = author?.let { listOf(it) } ?: emptyList(),
            links = listOf(
                WebLink(
                    "Kodansha",
                    kodanshaBaseUrl + "series/${series.id}"
                )
            )
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
            releaseDate = book.readable.digitalReleaseDate?.toLocalDate() ?: book.readable.printReleaseDate?.toLocalDate(),
            isbn = book.readable.eisbn ?: book.readable.isbn,
            authors = author?.let { listOf(it) } ?: emptyList(),
            thumbnail = thumbnail,
            links = listOf(WebLink("Kodansha", kodanshaBaseUrl + "product/${book.id}"))
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.toString()),
            metadata = metadata
        )

        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun parseDescription(description: String): String {
        return Jsoup.parse(description).wholeText().replace("\n\n", "\n")
    }
}
