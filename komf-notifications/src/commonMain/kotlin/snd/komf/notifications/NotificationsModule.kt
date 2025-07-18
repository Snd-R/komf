package snd.komf.notifications

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import snd.komf.ktor.HttpRequestRateLimiter
import snd.komf.ktor.komfUserAgent
import snd.komf.notifications.apprise.AppriseCliService
import snd.komf.notifications.apprise.AppriseVelocityTemplates
import snd.komf.notifications.discord.DiscordVelocityTemplates
import snd.komf.notifications.discord.DiscordWebhookService
import kotlin.time.Duration.Companion.seconds

class NotificationsModule(
    notificationsConfig: NotificationsConfig,
    ktorBaseClient: HttpClient,
) {
    private val discordConfig = notificationsConfig.discord
    private val appriseConfig = notificationsConfig.apprise

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    private val discordKtorClient = ktorBaseClient.config {
        install(HttpRequestRetry) {
            retryIf(3) { _, response ->
                when (response.status.value) {
                    HttpStatusCode.Companion.TooManyRequests.value -> true
                    in 500..599 -> true
                    else -> false
                }
            }
            exponentialDelay(respectRetryAfterHeader = true)
        }
        install(HttpRequestRateLimiter) {
            interval = 2.seconds
            eventsPerInterval = 4
            allowBurst = false
        }
        install(UserAgent) { agent = komfUserAgent }
        install(ContentNegotiation) { json(json) }
    }

    val appriseVelocityRenderer = AppriseVelocityTemplates(notificationsConfig.templatesDirectory)
    val appriseService = AppriseCliService(
        urls = notificationsConfig.apprise.urls ?: emptyList(),
        templateRenderer = appriseVelocityRenderer,
        seriesCover = appriseConfig.seriesCover,
    )

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