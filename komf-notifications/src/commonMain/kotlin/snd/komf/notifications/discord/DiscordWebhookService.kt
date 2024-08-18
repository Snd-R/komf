package snd.komf.notifications.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import snd.komf.model.Image
import snd.komf.notifications.discord.model.Embed
import snd.komf.notifications.discord.model.EmbedFooter
import snd.komf.notifications.discord.model.EmbedImage
import snd.komf.notifications.discord.model.NotificationContext
import snd.komf.notifications.discord.model.Webhook
import snd.komf.notifications.discord.model.WebhookExecuteRequest

private val logger = KotlinLogging.logger {}

private const val baseUrl = "https://discord.com/api"

class DiscordWebhookService(
    private val ktor: HttpClient,
    private val json: Json,
    private val templateRenderer: VelocityTemplateService,
    private val seriesCover: Boolean,
    private val webhooks: Collection<String>,
    embedColor: String,
) {
    private val embedColor = embedColor.toInt(16)

    suspend fun send(context: NotificationContext) {
        val webhookRequest = toRequest(context) ?: return
        webhooks.map { getWebhook(it) }.forEach { webhook ->
            executeWebhook(
                webhook = webhook,
                webhookRequest = webhookRequest,
                image = if (seriesCover) context.seriesCover else null
            )
        }
    }

    private fun toRequest(context: NotificationContext): WebhookExecuteRequest? {
        val renderResult = templateRenderer.renderDiscord(context)
        if (renderResult.description == null &&
            renderResult.fields.isEmpty() &&
            renderResult.footer == null &&
            renderResult.title == null
            && !seriesCover
        ) {
            logger.warn { "empty discord message for series ${context.series.name}. Skipping notification" }
            return null
        }

        val image = if (seriesCover && context.seriesCover != null) {
            val contentType = context.seriesCover.mimeType?.replace("image/", "") ?: "jpeg"
            EmbedImage(url = "attachment://cover.$contentType")
        } else null

        val embed = Embed(
            title = renderResult.title,
            url = renderResult.titleUrl,
            description = renderResult.description,
            fields = renderResult.fields,
            footer = renderResult.footer?.let { EmbedFooter(text = it) },
            color = embedColor,
            image = image
        )
        return WebhookExecuteRequest(embeds = listOf(embed))
    }

    private suspend fun getWebhook(webhookUrl: String): Webhook {
        return ktor.get(webhookUrl).body()
    }

    private suspend fun executeWebhook(webhook: Webhook, webhookRequest: WebhookExecuteRequest, image: Image? = null) {
        ktor.post("$baseUrl/webhooks/${webhook.id}/${webhook.token}") {
            if (image == null) {
                contentType(ContentType.Application.Json)
                setBody(webhookRequest)
            } else {
                val filename = "cover.${image.mimeType?.replace("image/", "") ?: "jpeg"}"
                contentType(ContentType.MultiPart.FormData)
                setBody(
                    MultiPartFormDataContent(formData {
                        append(
                            "cover",
                            image.image,
                            Headers.build { append(HttpHeaders.ContentDisposition, "filename=\"$filename\"") }
                        )
                        append("payload_json", json.encodeToString(webhookRequest))
                    })
                )
            }

        }
    }
}
