package org.snd.metadata.providers.bookwalker

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Author
import org.snd.metadata.model.AuthorRole
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.Image
import org.snd.metadata.model.MediaServerWebLink
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.ReleaseDate
import org.snd.metadata.model.SeriesBook
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesTitle
import org.snd.metadata.model.TitleType.LOCALIZED
import org.snd.metadata.model.TitleType.NATIVE
import org.snd.metadata.providers.bookwalker.model.BookWalkerBook
import org.snd.metadata.providers.bookwalker.model.BookWalkerSeriesBook
import org.snd.metadata.providers.bookwalker.model.BookWalkerSeriesId
import java.net.URLEncoder

class BookWalkerMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {
    private val artistRoles = listOf(
        AuthorRole.PENCILLER,
        AuthorRole.INKER,
        AuthorRole.COLORIST,
        AuthorRole.LETTERER,
        AuthorRole.COVER
    )

    fun toSeriesMetadata(
        seriesId: BookWalkerSeriesId,
        book: BookWalkerBook,
        allBooks: Collection<BookWalkerSeriesBook>,
        thumbnail: Image? = null
    ): ProviderSeriesMetadata {
        val titles = listOfNotNull(
            book.seriesTitle?.let { SeriesTitle(it, LOCALIZED) },
            book.romajiTitle?.let { SeriesTitle(it, LOCALIZED) },
            book.japaneseTitle?.let { SeriesTitle(it, NATIVE) }
        )

        val metadata = SeriesMetadata(
            titles = titles,
            summary = book.synopsis,
            publisher = book.publisher,
            genres = book.genres,
            tags = emptyList(),
            totalBookCount = allBooks.size.let { if (it < 1) null else it },
            authors = getAuthors(book),
            thumbnail = thumbnail,
            releaseDate = ReleaseDate(
                year = book.availableSince?.year,
                month = book.availableSince?.monthValue,
                day = book.availableSince?.dayOfMonth,
            ),
            links = listOf(
                MediaServerWebLink(
                    "BookWalker",
                    bookWalkerBaseUrl + "series/${URLEncoder.encode(seriesId.id, "UTF-8")}"
                )
            )
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(seriesId.id),
            metadata = metadata,
            books = allBooks.map {
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

    fun toBookMetadata(book: BookWalkerBook, thumbnail: Image? = null): ProviderBookMetadata {
        val metadata = BookMetadata(
            title = book.name,
            summary = book.synopsis,
            number = book.number,
            releaseDate = book.availableSince,
            authors = getAuthors(book),
            startChapter = null,
            endChapter = null,
            thumbnail = thumbnail,
            links = listOf(
                MediaServerWebLink(
                    "BookWalker",
                    bookWalkerBaseUrl + URLEncoder.encode(book.id.id, "UTF-8")
                )
            )
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.id),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun getAuthors(book: BookWalkerBook): List<Author> {
        val artists = book.artists.flatMap { name -> artistRoles.map { role -> Author(name, role) } }
        val authors = book.authors.map { name -> Author(name, AuthorRole.WRITER) }
        return artists + authors
    }
}
