package snd.komf.notifications.discord.model

import snd.komf.model.Image

data class NotificationContext(
    val library: LibraryContext,
    val series: SeriesContext,
    val books: List<BookContext>,
    val mediaServer: String,
    val seriesCover: Image?,
)

data class LibraryContext(
    val id: String,
    val name: String
)

data class SeriesContext(
    val id: String,
    val name: String,
    val bookCount: Int,
    val metadata: SeriesMetadataContext
)

data class SeriesMetadataContext(
    val status: String,
    val title: String,
    val titleSort: String,
    val alternativeTitles: List<AlternativeTitleContext>,
    val summary: String,
    val readingDirection: String?,
    val publisher: String?,
    val alternativePublishers: Set<String>,
    val ageRating: Int?,
    val language: String?,
    val genres: List<String>,
    val tags: List<String>,
    val totalBookCount: Int?,
    val authors: List<AuthorContext>,
    val releaseYear: Int?,
    val links: List<WebLinkContext>,
)

data class BookContext(
    val id: String,
    val name: String,
    val number: Int,
    val metadata: BookMetadataContext
)

data class BookMetadataContext(
    val title: String,
    val summary: String?,
    val number: String,
    val numberSort: String?,
    val releaseDate: String?,
    val authors: List<AuthorContext>,
    val tags: List<String>,
    val isbn: String?,
    val links: List<WebLinkContext>,
)

data class AlternativeTitleContext(
    val label: String,
    val title: String,
)

data class AuthorContext(
    val name: String,
    val role: String,
)

data class WebLinkContext(
    val label: String,
    val url: String,
)
