package snd.komf.providers.anilist.model

import kotlinx.serialization.Serializable
import snd.komf.providers.anilist.model.AniListMediaFormat

@Serializable
data class AniListSearchQuery(
    val search: String,
    val formats: List<AniListMediaFormat>,
    val perPage: Int
)

@Serializable
data class AniListMediaQuery(
    val id: Int
)