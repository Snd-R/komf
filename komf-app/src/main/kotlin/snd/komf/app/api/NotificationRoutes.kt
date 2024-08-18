package snd.komf.app.api

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.StateFlow
import snd.komf.api.KomfErrorResponse
import snd.komf.api.notifications.EmbedField
import snd.komf.api.notifications.EmbedFieldTemplate
import snd.komf.api.notifications.KomfDiscordTemplates
import snd.komf.api.notifications.KomfNotificationContext
import snd.komf.api.notifications.KomfRenderRequest
import snd.komf.api.notifications.KomfTemplateRenderResult
import snd.komf.notifications.discord.DiscordStringTemplates
import snd.komf.notifications.discord.DiscordWebhookService
import snd.komf.notifications.discord.FieldStringTemplates
import snd.komf.notifications.discord.VelocityTemplateService
import snd.komf.notifications.discord.model.AlternativeTitleContext
import snd.komf.notifications.discord.model.AuthorContext
import snd.komf.notifications.discord.model.BookContext
import snd.komf.notifications.discord.model.BookMetadataContext
import snd.komf.notifications.discord.model.LibraryContext
import snd.komf.notifications.discord.model.NotificationContext
import snd.komf.notifications.discord.model.SeriesContext
import snd.komf.notifications.discord.model.SeriesMetadataContext
import snd.komf.notifications.discord.model.WebLinkContext

class NotificationRoutes(
    private val notificationService: StateFlow<DiscordWebhookService?>,
    private val templateRenderer: StateFlow<VelocityTemplateService>
) {

    fun registerRoutes(routing: Route) {
        routing.route("/notifications/discord") {
            discordGetTemplatesRoute()
            discordUpdateTemplatesRoute()
            discordSendRoute()
            discordRenderRoute()
        }
    }

    private fun Route.discordUpdateTemplatesRoute() {
        post("/templates") {
            val request = call.receive<KomfDiscordTemplates>()
            templateRenderer.value.updateTemplates(request.toModel())

            call.respond(HttpStatusCode.OK, request)
        }
    }

    private fun Route.discordGetTemplatesRoute() {
        get("/templates") {
            val templates = templateRenderer.value.getCurrentTemplates()
            call.respond(
                HttpStatusCode.OK, KomfDiscordTemplates(
                    titleTemplate = templates.titleTemplate,
                    titleUrlTemplate = templates.titleUrlTemplate,
                    descriptionTemplate = templates.descriptionTemplate,
                    fields = templates.fieldTemplates.map {
                        EmbedFieldTemplate(
                            it.nameTemplate,
                            it.valueTemplate,
                            it.inline
                        )
                    },
                    footerTemplate = templates.footerTemplate,
                )
            )
        }
    }

    private fun Route.discordSendRoute() {
        post("/send") {
            val service = notificationService.value
            if (service == null) {
                call.respond(HttpStatusCode.UnprocessableEntity, KomfErrorResponse("No discord webhooks configured"))
            } else {
                val request = call.receive<KomfNotificationContext>()
                service.send(request.toModel())
                call.respond(HttpStatusCode.OK, "")
            }
        }
    }

    private fun Route.discordRenderRoute() {
        post("/render") {
            val request = call.receive<KomfRenderRequest>()
            val result = templateRenderer.value.renderDiscord(
                context = request.context.toModel(),
                templates = request.templates.toModel()
            )

            call.respond(
                HttpStatusCode.OK,
                KomfTemplateRenderResult(
                    title = result.title,
                    titleUrl = result.titleUrl,
                    description = result.description,
                    fields = result.fields.map { EmbedField(it.name, it.value, it.inline) },
                    footer = result.footer
                )
            )
        }
    }


    private fun KomfNotificationContext.toModel() = NotificationContext(
        library = LibraryContext(id = library.id, name = library.name),
        series = SeriesContext(
            id = series.id,
            name = series.name,
            bookCount = series.bookCount,
            metadata = SeriesMetadataContext(
                status = series.metadata.status,
                title = series.metadata.title,
                titleSort = series.metadata.titleSort,
                alternativeTitles = series.metadata.alternativeTitles.map {
                    AlternativeTitleContext(it.label, it.title)
                },
                summary = series.metadata.summary,
                readingDirection = series.metadata.readingDirection,
                publisher = series.metadata.publisher,
                alternativePublishers = series.metadata.alternativePublishers,
                ageRating = series.metadata.ageRating,
                language = series.metadata.language,
                genres = series.metadata.genres,
                tags = series.metadata.tags,
                totalBookCount = series.metadata.totalBookCount,
                authors = series.metadata.authors.map { AuthorContext(it.name, it.role) },
                releaseYear = series.metadata.releaseYear,
                links = series.metadata.links.map { WebLinkContext(it.label, it.url) },
            )
        ),
        books = books.map { book ->
            BookContext(
                id = book.id,
                name = book.name,
                number = book.number,
                metadata = BookMetadataContext(
                    title = book.metadata.title,
                    summary = book.metadata.summary,
                    number = book.metadata.number,
                    numberSort = book.metadata.numberSort,
                    releaseDate = book.metadata.releaseDate,
                    authors = book.metadata.authors.map { AuthorContext(it.name, it.role) },
                    tags = book.metadata.tags,
                    isbn = book.metadata.isbn,
                    links = book.metadata.links.map { WebLinkContext(it.label, it.url) },
                )
            )
        },
        mediaServer = mediaServer,
        seriesCover = null
    )

    private fun KomfDiscordTemplates.toModel() = DiscordStringTemplates(
        titleTemplate = titleTemplate,
        titleUrlTemplate = titleUrlTemplate,
        descriptionTemplate = descriptionTemplate,
        fieldTemplates = fields.map {
            FieldStringTemplates(
                nameTemplate = it.nameTemplate,
                valueTemplate = it.valueTemplate,
                inline = it.inline
            )
        },
        footerTemplate = footerTemplate
    )
}