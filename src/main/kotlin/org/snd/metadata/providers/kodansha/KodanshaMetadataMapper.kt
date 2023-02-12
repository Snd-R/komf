package org.snd.metadata.providers.kodansha

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.BookRange
import org.snd.metadata.model.Image
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesBook
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesStatus
import org.snd.metadata.model.SeriesTitle
import org.snd.metadata.model.TitleType
import org.snd.metadata.model.WebLink
import org.snd.metadata.providers.kodansha.model.KodanshaBook
import org.snd.metadata.providers.kodansha.model.KodanshaSeries
import org.snd.metadata.providers.kodansha.model.Status.COMPLETE
import org.snd.metadata.providers.kodansha.model.Status.COMPLETED
import org.snd.metadata.providers.kodansha.model.Status.ONGOING
import java.net.URLEncoder

class KodanshaMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {

    fun toSeriesMetadata(series: KodanshaSeries, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            ONGOING -> SeriesStatus.ONGOING
            COMPLETED -> SeriesStatus.ENDED
            COMPLETE -> SeriesStatus.ENDED
            else -> null
        }
        val metadata = SeriesMetadata(
            status = status,
            titles = listOf(SeriesTitle(series.title, TitleType.LOCALIZED, "en")),
            summary = series.summary,
            publisher = series.publisher,
            ageRating = series.ageRating,
            tags = series.tags,
            totalBookCount = if (series.books.isEmpty()) null else series.books.size,
            thumbnail = thumbnail,
            links = listOf(
                WebLink(
                    "Kodansha",
                    kodanshaBaseUrl + "series/${URLEncoder.encode(series.id.id, "UTF-8")}"
                )
            )
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(series.id.id),
            metadata = metadata,
            books = series.books.map {
                SeriesBook(
                    id = ProviderBookId(it.id.id),
                    number = it.number?.let { number -> BookRange(number.toDouble(), number.toDouble()) },
                    name = "${series.title} ${it.number}",
                    type = null,
                    edition = null
                )
            }
        )
        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(book: KodanshaBook, thumbnail: Image? = null): ProviderBookMetadata {
        val metadata = BookMetadata(
            title = book.name,
            summary = book.summary,
            number = book.number?.let { number -> BookRange(number.toDouble(), number.toDouble()) },
            releaseDate = book.ebookReleaseDate ?: book.printReleaseDate,
            tags = book.tags.toSet(),
            isbn = book.eisbn ?: book.isbn,
            thumbnail = thumbnail,
            links = listOf(WebLink("Kodansha", kodanshaBaseUrl + "volume/${book.id.id}"))
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.id),
            metadata = metadata
        )

        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }
}
