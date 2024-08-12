package snd.komf.providers.anilist.model

import kotlinx.serialization.Serializable

@Serializable
data class AniListMedia(
    val id: Int,
    val title: AniListTitle? = null,
    val type: AniListMediaFormat? = null,
    val format: AniListMediaFormat? = null,
    val status: AniListMediaStatus? = null,
    val description: String? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val coverImage: AniListMediaCoverImage? = null,
    val startDate: AniListFuzzyDate? = null,
    val genres: List<String>? = null,
    val synonyms: List<String>? = null,
    val tags: List<AniListMediaTag>? = null,
    val staff: AniListStaffConnection? = null,
    val meanScore: Int? = null,

    )

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
)

@Serializable
enum class AniListMediaFormat {
    TV,
    TV_SHORT,
    MOVIE,
    SPECIAL,
    OVA,
    ONA,
    MUSIC,
    MANGA,
    NOVEL,
    ONE_SHOT,
}

@Serializable
enum class AniListMediaType {
    MANGA,
    ANIME,
}


@Serializable
enum class AniListMediaStatus {
    FINISHED,
    RELEASING,
    NOT_YET_RELEASED,
    CANCELLED,
    HIATUS
}

@Serializable
data class AniListMediaCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
    val color: String? = null,
)

@Serializable
data class AniListFuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)

@Serializable
data class AniListMediaTag(
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val rank: Int? = null
)

@Serializable
data class AniListStaffConnection(
    val edges: List<AniListStaffEdge>
)

@Serializable
data class AniListStaffEdge(
    val node: AniListStaff? = null,
    val role: String? = null,
)

@Serializable
data class AniListStaff(
    val name: AniListStaffName? = null,
    val languageV2: String? = null
)

@Serializable
data class AniListStaffName(
    val full: String? = null
)