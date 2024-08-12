package snd.komf.notifications.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeSingleton
import snd.komf.model.Image
import snd.komf.notifications.discord.model.Embed
import snd.komf.notifications.discord.model.EmbedField
import snd.komf.notifications.discord.model.EmbedFooter
import snd.komf.notifications.discord.model.EmbedImage
import snd.komf.notifications.discord.model.Webhook
import snd.komf.notifications.discord.model.WebhookExecuteRequest
import snd.komf.notifications.discord.model.WebhookMessage
import snd.komf.notifications.discord.model.toVelocityContext
import java.io.StringReader
import java.io.StringWriter

private val logger = KotlinLogging.logger {}

private const val baseUrl = "https://discord.com/api"

class DiscordWebhookService(
    private val ktor: HttpClient,
    private val json: Json,

    private val seriesCover: Boolean,
    private val webhooks: Collection<String>,
    title: String?,
    titleUrl: String?,
    descriptionTemplateName: String?,
    fieldTemplateConfigs: List<DiscordFieldTemplateConfig>,
    footerTemplateName: String?,
    colorCode: String,
    velocityEngine: VelocityEngine,
) {
    private val color = colorCode.toInt(16)
    private val titleTemple = title?.let { templateFromString(it) }
    private val titleUrlTemple = titleUrl?.let { templateFromString(it) }
    private val descriptionTemplate = descriptionTemplateName?.let { velocityEngine.getTemplate(it) }
    private val footerTemplate = footerTemplateName?.let { velocityEngine.getTemplate(it) }
    private val fieldTemplates = fieldTemplateConfigs.map {
        FieldTemplate(
            value = velocityEngine.getTemplate(it.templateName),
            name = templateFromString(it.name),
            inline = it.inline
        )
    }

    suspend fun send(message: WebhookMessage) {
        val webhookRequest = toRequest(message) ?: return
        webhooks.map { getWebhook(it) }.forEach { webhook ->
            executeWebhook(
                webhook = webhook,
                webhookRequest = webhookRequest,
                image = if (seriesCover) message.seriesCover else null
            )
        }
    }

    private fun toRequest(message: WebhookMessage): WebhookExecuteRequest? {
        val context = message.toVelocityContext()
        val title = titleTemple?.let { renderTemplate(it, context).take(256) }
        val titleUrl = titleUrlTemple?.let { renderTemplate(it, context) }?.trim()
        val description = descriptionTemplate?.let { renderTemplate(it, context).take(4095) }
        val fields = fieldTemplates.map {
            EmbedField(
                name = renderTemplate(it.name, context).take(256),
                value = renderTemplate(it.value, context).take(1024),
                inline = it.inline
            )
        }
        val footer = footerTemplate?.let { renderTemplate(it, context).take(2048) }

        if (description == null && fields.isEmpty() && footer == null && title == null && !seriesCover) {
            logger.warn { "empty discord message for series ${message.series.name}. Skipping notification" }
            return null
        }

        val image = if (seriesCover && message.seriesCover != null) {
            val contentType = message.seriesCover.mimeType?.replace("image/", "") ?: "jpeg"
            EmbedImage(url = "attachment://cover.$contentType")
        } else null

        val embed = Embed(
            title = title,
            url = titleUrl,
            description = description,
            fields = fields,
            footer = footer?.let { EmbedFooter(text = it) },
            color = color,
            image = image
        )
        return WebhookExecuteRequest(embeds = listOf(embed))
    }

    private fun renderTemplate(template: Template, context: VelocityContext): String {
        return StringWriter().use {
            template.merge(context, it)
            it.toString()
        }
    }

    private fun templateFromString(template: String) = Template().apply {
        setRuntimeServices(RuntimeSingleton.getRuntimeServices())
        data = RuntimeSingleton.getRuntimeServices().parse(StringReader(template), this)
        initDocument()
    }

    private data class FieldTemplate(
        val value: Template,
        val name: Template,
        val inline: Boolean
    )

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
