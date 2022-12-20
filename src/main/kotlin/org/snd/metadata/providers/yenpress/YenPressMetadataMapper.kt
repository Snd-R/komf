package org.snd.metadata.providers.yenpress

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.*
import org.snd.metadata.model.TitleType.LOCALIZED
import org.snd.metadata.providers.yenpress.model.YenPressBook

class YenPressMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {
    fun toSeriesMetadata(book: YenPressBook, thumbnail: Image? = null): ProviderSeriesMetadata {
        val metadata = SeriesMetadata(
            status = null,
            titles = listOf(
                SeriesTitle(
                    book.name
                        .replace(", Vol. [0-9]+".toRegex(), "")
                        .removeSuffix(" (manga)"),
                    LOCALIZED
                )
            ),
            summary = book.description,
            publisher = book.publisher,
            genres = book.genres,
            tags = emptyList(),
            authors = emptyList(),
            thumbnail = thumbnail,
            totalBookCount = book.seriesBooks.size.let { if (it < 1) null else it },
            ageRating = null,
            releaseDate = book.releaseDate?.toReleaseDate()
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(book.id.id),
            metadata = metadata,
            books = book.seriesBooks.map {
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

    fun toBookMetadata(book: YenPressBook, thumbnail: Image? = null): ProviderBookMetadata {
        val metadata = BookMetadata(
            title = book.name,
            summary = book.description,
            number = book.number,
            releaseDate = book.releaseDate,
            isbn = book.isbn,
            startChapter = null,
            endChapter = null,
            thumbnail = thumbnail
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.id),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }
}
