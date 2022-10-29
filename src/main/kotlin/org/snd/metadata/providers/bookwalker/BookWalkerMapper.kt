package org.snd.metadata.providers.bookwalker

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.*
import org.snd.metadata.providers.bookwalker.model.BookWalkerBook
import org.snd.metadata.providers.bookwalker.model.BookWalkerSeriesBook
import org.snd.metadata.providers.bookwalker.model.BookWalkerSeriesId

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
        val metadata = SeriesMetadata(
            title = book.seriesTitle,
            titleSort = book.seriesTitle,
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
            thumbnail = thumbnail
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
