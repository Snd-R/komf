package org.snd.metadata.yenpress

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.Provider.YEN_PRESS
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesBook
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.Thumbnail
import org.snd.metadata.yenpress.model.YenPressBook

class YenPressMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {
    fun toSeriesMetadata(book: YenPressBook, thumbnail: Thumbnail? = null): ProviderSeriesMetadata {
        val metadata = SeriesMetadata(
            status = null,
            title = book.name,
            titleSort = book.name,
            summary = book.description,
            publisher = book.publisher,
            genres = book.genres,
            tags = null,
            authors = null,
            thumbnail = thumbnail,
            totalBookCount = book.seriesBooks.size,
            ageRating = null
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(book.id.id),
            provider = YEN_PRESS,
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

    fun toBookMetadata(book: YenPressBook, thumbnail: Thumbnail? = null): ProviderBookMetadata {
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
            provider = YEN_PRESS,
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }
}
