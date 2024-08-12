package snd.komf.app.module

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import snd.komf.app.config.NotificationsConfig
import snd.komf.ktor.komfUserAgent
import snd.komf.notifications.discord.DiscordWebhookService
import java.util.*


class NotificationsModule(
    komgaNotificationsConfig: NotificationsConfig,
    ktorBaseClient: HttpClient,
) {
    private val discordConfig = komgaNotificationsConfig.discord
    private val templateEngine: VelocityEngine = VelocityEngine().apply {
        val p = Properties()
        p.setProperty("resource.loaders", "file,class")
        p.setProperty("resource.loader.class.class", ClasspathResourceLoader::class.java.name)
        p.setProperty("resource.loader.file.path", discordConfig.templatesDirectory)

        init(p)
    }

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

    val discordWebhookService = discordConfig.webhooks?.let { webhooks ->
        DiscordWebhookService(
            ktor = discordKtorClient,
            json = json,
            seriesCover = discordConfig.seriesCover,
            webhooks = webhooks,
            title = discordConfig.title,
            titleUrl = discordConfig.titleUrl,
            descriptionTemplateName = discordConfig.descriptionTemplate,
            fieldTemplateConfigs = discordConfig.fieldTemplates,
            footerTemplateName = discordConfig.footerTemplate,
            colorCode = discordConfig.colorCode,
            velocityEngine = templateEngine
        )
    }
}