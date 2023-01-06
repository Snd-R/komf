package org.snd.noifications.discord

import mu.KotlinLogging
import org.apache.velocity.app.VelocityEngine
import org.snd.noifications.discord.client.DiscordClient
import org.snd.noifications.discord.model.Embed
import org.snd.noifications.discord.model.EmbedImage
import org.snd.noifications.discord.model.WebhookExecuteRequest
import org.snd.noifications.discord.model.WebhookMessage
import org.snd.noifications.discord.model.toVelocityContext
import org.snd.noifications.imgur.ImgurClient
import java.io.StringWriter

private val logger = KotlinLogging.logger {}

class DiscordWebhookService(
    private val webhooks: Collection<String>,
    private val discordClient: DiscordClient,
    private val seriesCover: Boolean,
    private val imgurClient: ImgurClient?,
    velocityEngine: VelocityEngine,
) {
    private val template = velocityEngine.getTemplate("discordWebhook.vm")

    fun send(message: WebhookMessage) {
        webhooks.map { discordClient.getWebhook(it) }
            .forEach { webhook -> toRequest(message)?.let { discordClient.executeWebhook(webhook, it) } }
    }

    private fun toRequest(message: WebhookMessage): WebhookExecuteRequest? {
        val description = renderTemplate(message).take(4095)

        if (description.isBlank()) {
            logger.warn { "empty discord message for series ${message.series.name}. Skipping notification" }
            return null
        }

        val imgurImage = runCatching {
            if (seriesCover && isNotOverLimit())
                message.seriesCover?.let { imgurClient!!.uploadImage(it) }?.data
            else null
        }
            .onFailure { logger.error(it) { } }
            .getOrNull()

        val embed = Embed(
            description = description,
            color = "1F8B4C".toInt(16),
            image = imgurImage?.let { EmbedImage(url = it.link) }
        )
        return WebhookExecuteRequest(embeds = listOf(embed))
    }

    private fun renderTemplate(message: WebhookMessage): String {
        return StringWriter().use {
            template.merge(message.toVelocityContext(), it)
            it.toString()
        }
    }

    private fun isNotOverLimit(): Boolean {
        return if (imgurClient != null) {
            val credits = imgurClient.getCredits().data
            credits.userLimit > 10 && credits.clientLimit > 10
        } else false
    }
}
