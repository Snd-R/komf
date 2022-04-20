package org.snd.komga.webhook

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.snd.infra.HttpClient
import org.snd.infra.MEDIA_TYPE_JSON

class DiscordClient(
    private val client: HttpClient,
    private val moshi: Moshi,
) {
    private val baseUrl = "https://discord.com/api".toHttpUrl()

    fun getWebhook(webhook: String): Webhook {
        val request = Request.Builder()
            .url(webhook.toHttpUrl())
            .build()

        return parseJson(client.execute(request))
    }

    fun executeWebhook(webhook: Webhook, webhookRequest: WebhookExecuteRequest) {
        val postBody = toJson(webhookRequest)
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("webhooks/${webhook.id}/${webhook.token}")
                    .build()
            )
            .post(postBody.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.execute(request)
    }

    private inline fun <reified T : Any> parseJson(json: String): T {
        return moshi.adapter<T>().lenient().fromJson(json) ?: throw RuntimeException()
    }

    private inline fun <reified T : Any> toJson(value: T): String {
        return moshi.adapter<T>().lenient().toJson(value)
    }
}
