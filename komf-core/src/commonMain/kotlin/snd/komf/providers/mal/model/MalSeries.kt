package snd.komf.providers.mal.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MalSeries(
    val id: Int,
    val title: String,
    @SerialName("main_picture")
    val mainPicture: MalPicture? = null,
    @SerialName("alternative_titles")
    val alternativeTitles: MalAlternativeTiltle? = null,
    @SerialName("start_date")
    val startDate: String? = null,
    @SerialName("end_date")
    val endDate: String? = null,
    val synopsis: String? = null,
    val mean: Double? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    @SerialName("num_list_users")
    val numListUsers: Int,
    @SerialName("num_scoring_users")
    val numScoringUsers: Int,
    val nsfw: MalNSFW? = null,
    val genres: Set<MalGenre> = emptySet(),
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
    @SerialName("media_type")
    val mediaType: MalMediaType,
    val status: MalStatus,
    @SerialName("num_volumes")
    val numVolumes: Int,
    @SerialName("num_chapters")
    val numChapters: Int,
    val authors: List<MalAuthor> = emptyList(),
    val pictures: List<MalPicture> = emptyList(),
    val background: String? = null,
    val serialization: List<MalSerialization> = emptyList(),
)

@Serializable
data class MalAlternativeTiltle(
    val synonyms: List<String>,
    val en: String,
    val ja: String
)

@Serializable
data class MalGenre(
    val id: Int,
    val name: String
)

@Serializable
data class MalPicture(
    val large: String?,
    val medium: String
)

@Serializable
data class MalAuthor(
    val node: MalAuthorNode,
    val role: String
)

@Serializable
data class MalAuthorNode(
    val id: Int,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
)

@Serializable
data class MalSerialization(
    val node: MalSerializationNode
)

@Serializable
data class MalSerializationNode(
    val id: Int,
    val name: String
)

enum class MalMediaType {
    UNKNOWN,
    MANGA,
    NOVEL,
    ONE_SHOT,
    DOUJINSHI,
    MANHWA,
    MANHUA,
    OEL,
    LIGHT_NOVEL

}

enum class MalStatus {
    FINISHED,
    CURRENTLY_PUBLISHING,
    NOT_YET_PUBLISHED,
    ON_HIATUS,
    DISCONTINUED
}

enum class MalNSFW {
    WHITE,
    GRAY,
    BLACK
}

