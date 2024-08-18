package snd.komf.api.notifications

import kotlinx.serialization.Serializable

@Serializable
data class KomfNotificationContext(
    val library: KomfLibraryContext = KomfLibraryContext(),
    val series: KomfSeriesContext = KomfSeriesContext(),
    val books: List<KomfBookContext> = listOf(KomfBookContext()),
    val mediaServer: String = "KOMGA",
)

@Serializable
data class KomfLibraryContext(
    val id: String = "1",
    val name: String = "Test library"
)

@Serializable
data class KomfSeriesContext(
    val id: String = "2",
    val name: String = "Test Series",
    val bookCount: Int = 1,
    val metadata: KomfSeriesMetadataContext = KomfSeriesMetadataContext()
)

@Serializable
data class KomfSeriesMetadataContext(
    val status: String = "ONGOING",
    val title: String = "Series Test",
    val titleSort: String = "Series Test",
    val alternativeTitles: List<KomfAlternativeTitleContext> = emptyList(),
    val summary: String = "Test Summary",
    val readingDirection: String? = "LEFT_TO_RIGHT",
    val publisher: String? = "Test Publisher",
    val alternativePublishers: Set<String> = emptySet(),
    val ageRating: Int? = 18,
    val language: String? = null,
    val genres: List<String> = listOf("genre1", "genre2"),
    val tags: List<String> = listOf("tag1", "tag2"),
    val totalBookCount: Int? = 2,
    val authors: List<KomfAuthorContext> = listOf(
        KomfAuthorContext(name = "Author1", role = "Writer"),
        KomfAuthorContext(name = "Author2", role = "Artist")
    ),
    val releaseYear: Int? = 2000,
    val links: List<KomfWebLinkContext> = listOf(
        KomfWebLinkContext("series link", "http://example.org")
    ),
)

@Serializable
data class KomfBookContext(
    val id: String = "1",
    val name: String = "Test Book",
    val number: Int = 1,
    val metadata: KomfBookMetadataContext = KomfBookMetadataContext()
)

@Serializable
data class KomfBookMetadataContext(
    val title: String = "Book Test",
    val summary: String? = "Book summary",
    val number: String = "1",
    val numberSort: String? = "1",
    val releaseDate: String? = "2000-12-03",
    val authors: List<KomfAuthorContext> = listOf(
        KomfAuthorContext(name = "Author1", role = "Writer"),
        KomfAuthorContext(name = "Author2", role = "Artist")
    ),
    val tags: List<String> = listOf("bookTag1", "bookTag2"),
    val isbn: String? = "9780000000000",
    val links: List<KomfWebLinkContext> = listOf(
        KomfWebLinkContext("book link", "http://example.org")
    ),
)

@Serializable
data class KomfAlternativeTitleContext(
    val label: String,
    val title: String,
)

@Serializable
data class KomfAuthorContext(
    val name: String,
    val role: String,
)

@Serializable
data class KomfWebLinkContext(
    val label: String,
    val url: String,
)
