package org.snd.discord

import mu.KotlinLogging
import org.apache.velocity.app.VelocityEngine
import org.snd.discord.client.DiscordClient
import org.snd.discord.model.Embed
import org.snd.discord.model.WebhookExecuteRequest
import org.snd.discord.model.WebhookMessage
import org.snd.discord.model.toVelocityContext
import java.io.StringWriter

private val logger = KotlinLogging.logger {}

class DiscordWebhookService(
    private val webhooks: Collection<String>,
    private val discordClient: DiscordClient,
    velocityEngine: VelocityEngine,
) {
    private val template = velocityEngine.getTemplate("discordWebhook.vm")

    fun send(message: WebhookMessage) {
        webhooks.map { discordClient.getWebhook(it) }
            .forEach { webhook ->
                toRequest(message)
                    .forEach { discordClient.executeWebhook(webhook, it) }
            }
    }

    private fun toRequest(message: WebhookMessage): Collection<WebhookExecuteRequest> {
        val description = renderTemplate(message).take(4095)

        if (description.isBlank()) {
            logger.warn { "empty discord message for series ${message.series.name}. Skipping notification" }
            return emptyList()
        }

        val embed = Embed(
            description = description,
            color = "1F8B4C".toInt(16),
        )
        return listOf(WebhookExecuteRequest(embeds = listOf(embed)))
    }

    private fun renderTemplate(message: WebhookMessage): String {
        return StringWriter().use {
            template.merge(message.toVelocityContext(), it)
            it.toString()
        }
    }
}
