package snd.komf.app.module

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import snd.komf.app.config.NotificationsConfig
import snd.komf.ktor.komfUserAgent
import snd.komf.notifications.discord.DiscordWebhookService
import snd.komf.notifications.discord.VelocityTemplateService


class NotificationsModule(
    komgaNotificationsConfig: NotificationsConfig,
    ktorBaseClient: HttpClient,
) {
    private val discordConfig = komgaNotificationsConfig.discord

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    private val discordKtorClient = ktorBaseClient.config {
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay(respectRetryAfterHeader = true)
        }
        install(UserAgent) { agent = komfUserAgent }
        install(ContentNegotiation) { json(json) }
    }

    val velocityRenderer = VelocityTemplateService(discordConfig.templatesDirectory)
    val discordWebhookService = discordConfig.webhooks?.let { webhooks ->
        if (webhooks.isEmpty()) null
        else
            DiscordWebhookService(
                ktor = discordKtorClient,
                json = json,
                templateRenderer = velocityRenderer,
                seriesCover = discordConfig.seriesCover,
                webhooks = webhooks,
                embedColor = discordConfig.embedColor,
            )
    }
}