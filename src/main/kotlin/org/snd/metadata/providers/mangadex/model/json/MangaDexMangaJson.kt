package org.snd.metadata.providers.mangadex.model.json

import com.squareup.moshi.JsonClass
import org.snd.metadata.providers.mangadex.model.MangaDexTag
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
data class MangaDexMangaJson(
    val id: String,
    val type: String,
    val attributes: MangaDexAttributesJson,
    val relationships: List<MangaDexRelationshipJson>
)

@JsonClass(generateAdapter = true)
data class MangaDexAttributesJson(
    val title: Map<String, String>,
    val altTitles: List<Map<String, String>>,
    val description: Map<String, String>,
    val isLocked: Boolean,
    val links: Map<String, String>?,
    val originalLanguage: String,
    val lastVolume: String?,
    val lastChapter: String?,
    val publicationDemographic: String?,
    val status: String,
    val year: Int?,
    val contentRating: String,
    val tags: List<MangaDexTag>,
    val state: String,
    val chapterNumbersResetOnNewVolume: Boolean,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val version: Int,
    val availableTranslatedLanguages: List<String>,
    val latestUploadedChapter: String?,
)

@JsonClass(generateAdapter = true)
data class MangaDexRelationshipJson(
    val id: String,
    val type: String,
    val attributes: Map<String, Any>?
)