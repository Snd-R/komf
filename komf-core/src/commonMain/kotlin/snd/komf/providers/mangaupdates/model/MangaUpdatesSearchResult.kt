package snd.komf.providers.mangaupdates.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    @SerialName("series_id")
    val id: Long,
    val title: String,
    val description: String?,
    val image: MangaUpdatesImage?,
    val genres: Collection<MangaUpdatesGenre>?,
    val year: String?,
    val url:String,
)


