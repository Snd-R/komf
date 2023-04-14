package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDate

data class Subject(
    val id: Long,
    val type: SubjectType,
    val name: String,
    val nameCn: String?,
    val summary: String,
    val nsfw: Boolean,
    val locked: Boolean,

    // TV, Web, 欧美剧, PS4...
    val platform: String,
    val images: Images,
    val rating: SubjectRating,
    val collection: SubjectCollection,
    val tags: List<SubjectTag>,

    val date: LocalDate?,

    val publisher: String?,
    val otherPublishers: Collection<String>,

    val endDateRaw: String?,
    val aliases: Collection<SubjectAlias>,
    val authors: Collection<SubjectAuthor>,
    val isbn: String?,
)

data class SubjectAuthor(
    val role: SubjectAuthorRole,
    val name: String,
)

enum class SubjectAuthorRole {
    AUTHOR,
    ILLUSTRATOR,
    ORIGINAL_CREATOR,
    CHARACTER_DESIGN

}

enum class SubjectType(val value: Int) {
    BOOK(1),
    ANIME(2),
    MUSIC(3),
    GAME(4),
    REAL(6);

    companion object {
        fun fromValue(value: Int): SubjectType {
            return values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid SubjectType value: $value")
        }
    }
}

data class SubjectAlias(
    val language: String?,
    val value: String
)

@JsonClass(generateAdapter = true)
data class SubjectTag(
    val name: String,
    val count: Int
)

@JsonClass(generateAdapter = true)
data class SubjectRelation(
    val id: Long,
    val name: String,
    @Json(name = "name_cn")
    val nameCn: String,
    val type: SubjectType,
    val relation: String,
    val images: RelationImages,
)

@JsonClass(generateAdapter = true)
data class RelationImages(
    val small: String,
    val grid: String,
    val large: String,
    val medium: String,
    val common: String,
)

@JsonClass(generateAdapter = true)
data class SubjectCollection(
    val wish: Int,
    val collect: Int,
    val doing: Int,
    @Json(name = "on_hold")
    val onHold: Int,
    val dropped: Int

)

@JsonClass(generateAdapter = true)
data class SubjectRating(
    val rank: Int,
    val total: Int,
    val count: Map<Int, Int>,
    val score: java.math.BigDecimal
)

@JsonClass(generateAdapter = true)
data class Images(
    val large: String,
    val common: String?,
    val medium: String,
    val small: String,
    val grid: String
)
