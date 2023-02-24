package org.snd.noifications.discord

import mu.KotlinLogging
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeSingleton
import org.snd.config.DiscordFieldTemplateConfig
import org.snd.noifications.discord.client.DiscordClient
import org.snd.noifications.discord.model.Embed
import org.snd.noifications.discord.model.EmbedField
import org.snd.noifications.discord.model.EmbedFooter
import org.snd.noifications.discord.model.EmbedImage
import org.snd.noifications.discord.model.WebhookExecuteRequest
import org.snd.noifications.discord.model.WebhookMessage
import org.snd.noifications.discord.model.toVelocityContext
import java.io.StringReader
import java.io.StringWriter

private val logger = KotlinLogging.logger {}

class DiscordWebhookService(
    private val discordClient: DiscordClient,
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

    fun send(message: WebhookMessage) {
        val webhookRequest = toRequest(message) ?: return
        webhooks.map { discordClient.getWebhook(it) }.forEach { webhook ->
            discordClient.executeWebhook(
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
}
