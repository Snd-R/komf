package org.snd.metadata.providers.kodansha

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.*
import org.snd.metadata.providers.kodansha.model.KodanshaBook
import org.snd.metadata.providers.kodansha.model.KodanshaSeries
import org.snd.metadata.providers.kodansha.model.Status.*

class KodanshaMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {

    fun toSeriesMetadata(series: KodanshaSeries, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            ONGOING -> SeriesStatus.ONGOING
            COMPLETED -> SeriesStatus.ENDED
            COMPLETE -> SeriesStatus.ENDED
            else -> SeriesStatus.ONGOING
        }
        val metadata = SeriesMetadata(
            status = status,
            title = series.title,
            titleSort = series.title,
            summary = series.summary,
            publisher = series.publisher,
            ageRating = series.ageRating,
            tags = series.tags,
            totalBookCount = series.books.size,
            thumbnail = thumbnail,
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(series.id.id),
            metadata = metadata,
            books = series.books.map {
                SeriesBook(
                    id = ProviderBookId(it.id.id),
                    number = it.number,
                    name = "${metadata.title} $it.number",
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
            number = book.number,
            releaseDate = book.ebookReleaseDate ?: book.printReleaseDate,
            tags = book.tags.toSet(),
            isbn = book.eisbn ?: book.isbn,
            thumbnail = thumbnail
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.id),
            metadata = metadata
        )

        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }
}
