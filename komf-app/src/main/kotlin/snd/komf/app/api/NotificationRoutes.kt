package snd.komf.app.api

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import snd.komf.api.KomfErrorResponse
import snd.komf.api.notifications.EmbedField
import snd.komf.api.notifications.EmbedFieldTemplate
import snd.komf.api.notifications.KomfAppriseRenderResult
import snd.komf.api.notifications.KomfAppriseRequest
import snd.komf.api.notifications.KomfAppriseTemplates
import snd.komf.api.notifications.KomfDiscordRenderResult
import snd.komf.api.notifications.KomfDiscordRequest
import snd.komf.api.notifications.KomfDiscordTemplates
import snd.komf.api.notifications.KomfNotificationContext
import snd.komf.notifications.apprise.AppriseCliService
import snd.komf.notifications.apprise.AppriseStringTemplates
import snd.komf.notifications.apprise.AppriseVelocityTemplates
import snd.komf.notifications.discord.DiscordStringTemplates
import snd.komf.notifications.discord.DiscordVelocityTemplates
import snd.komf.notifications.discord.DiscordWebhookService
import snd.komf.notifications.discord.FieldStringTemplates
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
    private val discordService: Flow<DiscordWebhookService>,
    private val discordRenderer: Flow<DiscordVelocityTemplates>,

    private val appriseService: Flow<AppriseCliService>,
    private val appriseRenderer: Flow<AppriseVelocityTemplates>,
) {

    fun registerRoutes(routing: Route) {
        routing.route("/notifications/discord") {
            discordGetTemplatesRoute()
            discordUpdateTemplatesRoute()
            discordSendRoute()
            discordRenderRoute()
        }
        routing.route("/notifications/apprise") {
            appriseGetTemplatesRoute()
            appriseUpdateTemplatesRoute()
            appriseSendRoute()
            appriseRenderRoute()
        }
    }

    private fun Route.discordUpdateTemplatesRoute() {
        post("/templates") {
            val request = call.receive<KomfDiscordTemplates>()
            try {
                discordRenderer.first().updateTemplates(request.toModel())
            } catch (exception: Exception) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    KomfErrorResponse("${exception::class.simpleName} :${exception.message}")
                )
            }

            call.respond(HttpStatusCode.OK, request)
        }
    }

    private fun Route.discordGetTemplatesRoute() {
        get("/templates") {
            val templates = discordRenderer.first().getCurrentTemplates()
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
            val request = call.receive<KomfDiscordRequest>()
            try {
                discordService.first().send(request.context.toModel(), request.templates.toModel())
            } catch (exception: ResponseException) {
                call.respond(
                    exception.response.status,
                    KomfErrorResponse("${exception::class.simpleName} ${exception.response.bodyAsText()}")
                )
            }
            call.respond(HttpStatusCode.OK, "")
        }
    }

    private fun Route.discordRenderRoute() {
        post("/render") {
            val request = call.receive<KomfDiscordRequest>()
            val result = discordRenderer.first().render(
                context = request.context.toModel(),
                templates = request.templates.toModel()
            )

            call.respond(
                HttpStatusCode.OK,
                KomfDiscordRenderResult(
                    title = result.title,
                    titleUrl = result.titleUrl,
                    description = result.description,
                    fields = result.fields.map { EmbedField(it.name, it.value, it.inline) },
                    footer = result.footer
                )
            )
        }
    }


    private fun Route.appriseUpdateTemplatesRoute() {
        post("/templates") {
            val request = call.receive<KomfAppriseTemplates>()
            try {
                appriseRenderer.first().updateTemplates(request.toModel())
            } catch (exception: Exception) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    KomfErrorResponse("${exception::class.simpleName} :${exception.message}")
                )
            }

            call.respond(HttpStatusCode.OK, request)
        }
    }

    private fun Route.appriseGetTemplatesRoute() {
        get("/templates") {
            val templates = appriseRenderer.first().getCurrentTemplates()
            call.respond(
                HttpStatusCode.OK, KomfAppriseTemplates(
                    titleTemplate = templates.titleTemplate,
                    bodyTemplate = templates.bodyTemplate
                )
            )
        }
    }

    private fun Route.appriseSendRoute() {
        post("/send") {
            val service = appriseService.first()
            val request = call.receive<KomfAppriseRequest>()
            try {
                service.send(
                    request.context.toModel(),
                    request.templates.toModel()
                )
            } catch (exception: Exception) {
                call.respond(
                    HttpStatusCode.UnprocessableEntity,
                    KomfErrorResponse("${exception::class.simpleName} ${exception.message}")
                )
            }

            call.respond(HttpStatusCode.OK, "")
        }
    }

    private fun Route.appriseRenderRoute() {
        post("/render") {
            val request = call.receive<KomfAppriseRequest>()
            val result = appriseRenderer.first().render(
                context = request.context.toModel(),
                templates = request.templates.toModel()
            )

            call.respond(HttpStatusCode.OK, KomfAppriseRenderResult(title = result.title, body = result.body))
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

    private fun KomfAppriseTemplates.toModel() = AppriseStringTemplates(
        titleTemplate = titleTemplate,
        bodyTemplate = bodyTemplate,
    )
}