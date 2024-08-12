package snd.komf.providers.anilist.model

import kotlinx.serialization.Serializable

@Serializable
data class AniListResponse<T>(
    val data: T
)

@Serializable
data class AniListMediaSearchResponse(
    val mediaSearch: AniListMediaSearchMedia
)

@Serializable
data class AniListMediaSearchMedia(
    val media: List<AniListMedia>
)

@Serializable
data class AniListMediaResponse(
    val media: AniListMedia
)
