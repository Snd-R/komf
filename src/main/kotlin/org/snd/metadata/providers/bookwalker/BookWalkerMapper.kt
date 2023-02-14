package org.snd.metadata.providers.bookwalker

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.metadata.Author
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.ReleaseDate
import org.snd.metadata.model.metadata.SeriesBook
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.TitleType.LOCALIZED
import org.snd.metadata.model.metadata.TitleType.NATIVE
import org.snd.metadata.model.metadata.TitleType.ROMAJI
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.providers.bookwalker.model.BookWalkerBook
import org.snd.metadata.providers.bookwalker.model.BookWalkerSeriesBook
import org.snd.metadata.providers.bookwalker.model.BookWalkerSeriesId
import java.net.URLEncoder

class BookWalkerMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(
        seriesId: BookWalkerSeriesId,
        book: BookWalkerBook,
        allBooks: Collection<BookWalkerSeriesBook>,
        thumbnail: Image? = null
    ): ProviderSeriesMetadata {
        val titles = listOfNotNull(
            book.seriesTitle?.let { SeriesTitle(it, LOCALIZED, "en") },
            book.romajiTitle?.let { SeriesTitle(it, ROMAJI, "ja-ro") },
            book.japaneseTitle?.let { SeriesTitle(it, NATIVE, "ja") }
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
                WebLink(
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
                WebLink(
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
        val authors = book.authors.flatMap { name -> authorRoles.map { role -> Author(name, role) } }
        return artists + authors
    }
}
