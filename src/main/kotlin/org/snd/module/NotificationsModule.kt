package org.snd.module

import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.retry.RetryConfig
import okhttp3.OkHttpClient
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import org.snd.common.exceptions.HttpException
import org.snd.common.http.HttpClient
import org.snd.common.http.LoggingInterceptor
import org.snd.config.DiscordConfig
import org.snd.noifications.discord.DiscordWebhookService
import org.snd.noifications.discord.client.DiscordClient
import java.time.Duration
import java.util.*


class NotificationsModule(
    config: DiscordConfig,
    okHttpClient: OkHttpClient,
    jsonModule: JsonModule,
) {
    private val templateEngine: VelocityEngine = VelocityEngine().apply {
        val p = Properties()
        p.setProperty("resource.loaders", "file,class")
        p.setProperty("resource.loader.class.class", ClasspathResourceLoader::class.java.name)
        p.setProperty("resource.loader.file.path", config.templatesDirectory)

        init(p)
    }

    private val discordHttpClient = okHttpClient
        .newBuilder()
        .addInterceptor(LoggingInterceptor())
        .build()

    private val discordRetryConfig = RetryConfig.custom<Any>()
        .intervalBiFunction { _, result ->
            if (result.isRight) {
                return@intervalBiFunction 5000
            }
            val exception = result.swap().get()
            return@intervalBiFunction if (exception is HttpException && exception.code == 429) {
                exception.headers["retry-after"]?.toLong() ?: 5000
            } else 5000
        }.build()

    private val discordClient: DiscordClient = DiscordClient(
        client = HttpClient(
            client = discordHttpClient,
            name = "Discord",
            rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(2))
                .limitForPeriod(4)
                .timeoutDuration(Duration.ofSeconds(2))
                .build(),
            retryConfig = discordRetryConfig
        ),
        moshi = jsonModule.moshi
    )

    val discordWebhookService: DiscordWebhookService? = config.webhooks?.let {
        DiscordWebhookService(
            discordClient = discordClient,
            webhooks = config.webhooks,
            seriesCover = config.seriesCover,
            title = config.title,
            titleUrl = config.titleUrl,
            descriptionTemplateName = config.descriptionTemplate,
            fieldTemplateConfigs = config.fieldTemplates,
            footerTemplateName = config.footerTemplate,
            colorCode = config.colorCode,
            velocityEngine = templateEngine
        )
    }
}
