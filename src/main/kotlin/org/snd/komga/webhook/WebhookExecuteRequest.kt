package org.snd.komga.webhook

import com.squareup.moshi.JsonClass
import java.time.LocalDate

@JsonClass(generateAdapter = true)
data class WebhookExecuteRequest(
    val content: String? = null,
    val username: String? = null,
    val avatar_url: String? = null,
    val tts: Boolean? = null,
    val embeds: Collection<Embed>? = null,
    val flags: Int? = null,
)

@JsonClass(generateAdapter = true)
data class Embed(
    val title: String? = null,
    val type: String? = null,
    val description: String? = null,
    val url: String? = null,
    val timestamp: LocalDate? = null,
    val color: Int? = null,
    val footer: EmbedFooter? = null,
    val image: EmbedImage? = null,
    val thumbnail: EmbedThumbnail? = null,
    val provider: EmbedProvider? = null,
    val author: EmbedAuthor? = null,
    val fields: Collection<EmbedField>? = null,
)

@JsonClass(generateAdapter = true)
data class EmbedFooter(
    val text: String,
    val icon_url: String? = null,
    val proxy_icon_url: String? = null,
)

@JsonClass(generateAdapter = true)
data class EmbedImage(
    val url: String,
    val proxy_url: String? = null,
    val height: Int? = null,
    val width: Int? = null
)

@JsonClass(generateAdapter = true)
data class EmbedThumbnail(
    val url: String,
    val proxy_url: String? = null,
    val height: Int? = null,
    val width: Int? = null
)

@JsonClass(generateAdapter = true)
data class EmbedProvider(
    val name: String? = null,
    val url: String? = null,
)

@JsonClass(generateAdapter = true)
data class EmbedAuthor(
    val name: String,
    val url: String? = null,
    val icon_url: String? = null,
    val proxy_icon_url: String? = null,
)

@JsonClass(generateAdapter = true)
data class EmbedField(
    val name: String,
    val value: String,
    val inline: Boolean? = null,
)
