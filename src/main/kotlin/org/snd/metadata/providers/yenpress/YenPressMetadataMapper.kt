package org.snd.metadata.providers.yenpress

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.SeriesBook
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.TitleType.LOCALIZED
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.model.metadata.toReleaseDate
import org.snd.metadata.providers.yenpress.model.YenPressBook
import java.net.URLEncoder

class YenPressMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {
    fun toSeriesMetadata(book: YenPressBook, thumbnail: Image? = null): ProviderSeriesMetadata {
        val metadata = SeriesMetadata(
            status = null,
            titles = listOf(
                SeriesTitle(
                    bookTitle(book.name),
                    LOCALIZED,
                    "en"
                )
            ),
            summary = book.description,
            publisher = book.imprint,
            genres = book.genres,
            tags = emptyList(),
            authors = emptyList(),
            thumbnail = thumbnail,
            totalBookCount = book.seriesBooks.size.let { if (it < 1) null else it },
            ageRating = null,
            releaseDate = book.releaseDate?.toReleaseDate(),
            links = listOf(WebLink("YenPress", yenPressBaseUrl + URLEncoder.encode(book.id.id, "UTF-8")))
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
            thumbnail = thumbnail,
            links = listOf(WebLink("YenPress", yenPressBaseUrl + book.id.id))
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.id),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }
}

fun bookTitle(name: String) = name
    .replace("(\\(light novel\\))|(\\(manga\\))".toRegex(), "")
    .replace(", Vol. [0-9]+".toRegex(), "")
    .trim()
