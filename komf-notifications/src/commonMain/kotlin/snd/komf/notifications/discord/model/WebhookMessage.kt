package snd.komf.notifications.discord.model

import org.apache.velocity.VelocityContext
import snd.komf.model.Image

data class WebhookMessage(
    val library: LibraryWebhookMessage,
    val series: SeriesWebhookMessage,
    val books: List<BookWebhookMessage>,
    val mediaServer: String,
    val seriesCover: Image?,
)

data class LibraryWebhookMessage(
    val id: String,
    val name: String
)

data class SeriesWebhookMessage(
    val id: String,
    val name: String,
    val bookCount: Int,
    val metadata: SeriesMetadataWebhookMessage
)

data class SeriesMetadataWebhookMessage(
    val status: String,
    val title: String,
    val titleSort: String,
    val alternativeTitles: List<AlternativeTitleWebhookMessage>,
    val summary: String,
    val readingDirection: String?,
    val publisher: String?,
    val alternativePublishers: Set<String>,
    val ageRating: Int?,
    val language: String?,
    val genres: List<String>,
    val tags: List<String>,
    val totalBookCount: Int?,
    val authors: List<AuthorWebhookMessage>,
    val releaseYear: Int?,
    val links: List<WebLinkWebhookMessage>,
)

data class BookWebhookMessage(
    val id: String,
    val name: String,
    val number: Int,
    val metadata: BookMetadataWebhookMessage
)

data class BookMetadataWebhookMessage(
    val title: String,
    val summary: String?,
    val number: String,
    val numberSort: String?,
    val releaseDate: String?,
    val authors: List<AuthorWebhookMessage>,
    val tags: List<String>,
    val isbn: String?,
    val links: List<WebLinkWebhookMessage>,
)

data class AlternativeTitleWebhookMessage(
    val label: String,
    val title: String,
)

data class AuthorWebhookMessage(
    val name: String,
    val role: String,
)

data class WebLinkWebhookMessage(
    val label: String,
    val url: String,
)

fun WebhookMessage.toVelocityContext(): VelocityContext {
    val context = VelocityContext()
    context.put("library", library)
    context.put("series", series)
    context.put("books", books.sortedBy { it.name })
    context.put("mediaServer", mediaServer)
    return context
}
