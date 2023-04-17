package org.snd.metadata.providers.yenpress

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
import org.snd.metadata.model.metadata.SeriesBook
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.TitleType.LOCALIZED
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.model.metadata.toReleaseDate
import org.snd.metadata.providers.yenpress.model.YenPressAuthor
import org.snd.metadata.providers.yenpress.model.YenPressBook
import org.snd.metadata.providers.yenpress.model.YenPressBookShort
import java.net.URLEncoder

class YenPressMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    fun toSeriesMetadata(
        book: YenPressBook,
        books: List<YenPressBookShort>,
        thumbnail: Image? = null
    ): ProviderSeriesMetadata {

        val metadata = SeriesMetadata(
            status = null,
            titles = listOf(
                SeriesTitle(
                    bookTitle(book.name),
                    LOCALIZED,
                    "en"
                )
            ),
            authors = authors(book.authors),
            summary = book.description,
            genres = book.genres,
            tags = emptyList(),
            releaseDate = book.releaseDate?.toReleaseDate(),
            ageRating = book.ageRating?.let { ageRating(it) },
            publisher = book.imprint,
            thumbnail = thumbnail,
            totalBookCount = books.ifEmpty { null }?.size,
            links = listOf(
                WebLink(
                    "YenPress",
                    "${yenPressBaseUrl}series/" + URLEncoder.encode(book.seriesId.id, "UTF-8")
                )
            )
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(book.id.id),
            metadata = metadata,
            books = books.map {
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
            number = book.number,
            title = book.name,
            authors = authors(book.authors),
            summary = book.description,
            releaseDate = book.releaseDate,
            isbn = book.isbn,
            startChapter = null,
            endChapter = null,
            thumbnail = thumbnail,
            links = listOf(WebLink("YenPress", "$yenPressBaseUrl/titles/${book.id.id}"))
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.id),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun authors(authors: List<YenPressAuthor>): List<Author> {
        return authors.flatMap { (role, name) ->
            when (role) {
                "Author", "Original author" -> authorRoles.map { Author(role = it, name = name) }
                "Illustrated by:", "Artist" -> artistRoles.map { Author(role = it, name = name) }
                "Created by" -> (authorRoles + artistRoles).map { Author(role = it, name = name) }
                "Translated by" -> listOf(Author(role = AuthorRole.TRANSLATOR, name = name))
                "Letterer" -> listOf(Author(role = AuthorRole.LETTERER, name = name))
                else -> emptyList()
            }
        }
    }

    private fun ageRating(ageRating: String): Int? {
        return when (ageRating) {
            "All Ages" -> 6
            "T (Teen)" -> 13
            "OT (Older Teen)" -> 16
            "18+ M (Mature)" -> 18
            else -> null
        }
    }
}

fun bookTitle(name: String) = name
    .replace("(\\(light novel\\))|(\\(manga\\))".toRegex(), "")
    .replace(", Vol. [0-9]+".toRegex(), "")
    .trim()
