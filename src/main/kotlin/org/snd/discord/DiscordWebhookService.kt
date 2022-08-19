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
    private val velocityEngine: VelocityEngine,
) {

    fun send(message: WebhookMessage) {
        webhooks.map { discordClient.getWebhook(it) }.forEach { webhook ->
            discordClient.executeWebhook(webhook, toRequest(message))
        }
    }

    private fun toRequest(message: WebhookMessage): WebhookExecuteRequest {
        val template = velocityEngine.getTemplate("discordWebhook.vm")
        val writer = StringWriter()
        template.merge(message.toVelocityContext(), writer)
        val description = writer.toString()
        if (description.isBlank()) {
            logger.warn { "empty discord message for series ${message.series.name}. Skipping notification" }
        }
        val embed = Embed(
            description = description,
            color = "1F8B4C".toInt(16),
        )
        return WebhookExecuteRequest(embeds = listOf(embed))
    }
}
