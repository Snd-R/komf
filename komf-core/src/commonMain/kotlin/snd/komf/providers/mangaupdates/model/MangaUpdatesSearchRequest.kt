package snd.komf.providers.mangaupdates.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaUpdatesSearchRequest(
    val search: String,
    val page: Int,
    val perpage: Int,
    val type: List<SeriesType>
)
