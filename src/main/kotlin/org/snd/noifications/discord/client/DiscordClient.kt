package org.snd.noifications.discord.client

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.snd.common.http.HttpClient
import org.snd.common.http.MEDIA_TYPE_JSON
import org.snd.metadata.model.Image
import org.snd.noifications.discord.model.Webhook
import org.snd.noifications.discord.model.WebhookExecuteRequest

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

    fun executeWebhook(webhook: Webhook, webhookRequest: WebhookExecuteRequest, image: Image? = null) {
        val jsonPayload = toJson(webhookRequest)
        val body = if (image == null) {
            jsonPayload.toRequestBody(MEDIA_TYPE_JSON)
        } else {
            val filename = "cover.${image.mimeType?.replace("image/", "") ?: "jpeg"}"
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("cover", filename, image.image.toRequestBody())
                .addFormDataPart("payload_json", jsonPayload)
                .build()
        }

        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("webhooks/${webhook.id}/${webhook.token}")
                    .build()
            )
            .post(body)
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
