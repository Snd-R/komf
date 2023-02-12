package org.snd.metadata.providers.mangadex.model.json

import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
data class MangaDexCoverArtJson(
    val id: String,
    val type: String,
    val attributes: MangaDexCoverArtAttributesJson
)

@JsonClass(generateAdapter = true)
data class MangaDexCoverArtAttributesJson(
    val volume: String?,
    val fileName: String,
    val description: String,
    val locale: String,
    val version: Int,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime
)