package snd.komf.providers.mal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MalSearchResults(
    val data: List<MalSearchNode>,
    val paging: MalPaging
)

@Serializable
data class MalSearchNode(
    val node: MalSearchResult,
)

@Serializable
data class MalSearchResult(
    val id: Int,
    val title: String,
    @SerialName("alternative_titles")
    val alternativeTitles: MalAlternativeTiltle,
    @SerialName("media_type")
    val mediaType: MalMediaType,
    @SerialName("main_picture")
    val mainPicture: MalPicture? = null,
)

@Serializable
data class MalPaging(
    val next: String? = null
)
