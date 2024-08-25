package snd.komf.app.module

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import snd.komf.app.config.NotificationsConfig
import snd.komf.ktor.komfUserAgent
import snd.komf.notifications.apprise.AppriseCliService
import snd.komf.notifications.apprise.AppriseVelocityTemplates
import snd.komf.notifications.discord.DiscordVelocityTemplates
import snd.komf.notifications.discord.DiscordWebhookService


class NotificationsModule(
    notificationsConfig: NotificationsConfig,
    ktorBaseClient: HttpClient,
) {
    private val discordConfig = notificationsConfig.discord

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

    val appriseVelocityRenderer = AppriseVelocityTemplates(notificationsConfig.templatesDirectory)
    val appriseService = AppriseCliService(notificationsConfig.apprise.urls ?: emptyList(), appriseVelocityRenderer)

    val discordVelocityRenderer = DiscordVelocityTemplates(notificationsConfig.templatesDirectory)
    val discordWebhookService = DiscordWebhookService(
        ktor = discordKtorClient,
        json = json,
        templateRenderer = discordVelocityRenderer,
        seriesCover = discordConfig.seriesCover,
        webhooks = discordConfig.webhooks ?: emptyList(),
        embedColor = discordConfig.embedColor,
    )
}