package snd.komf.providers.mangaupdates.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaUpdatesSearchRequest(
    val search: String,
    val page: Int,
    val perPage: Int,
    val type: List<SeriesType>
)
