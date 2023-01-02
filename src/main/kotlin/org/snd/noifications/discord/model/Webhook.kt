package org.snd.noifications.discord.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Webhook(
    val type: Int,
    val id: String,
    val name: String,
    val avatar: String?,
    val channel_id: String,
    val guild_id: String,
    val application_id: String?,
    val token: String,
)
