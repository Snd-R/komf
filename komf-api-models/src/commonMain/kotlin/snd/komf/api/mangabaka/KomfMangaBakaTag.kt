package snd.komf.api.mangabaka

import kotlinx.serialization.Serializable

@Serializable
data class KomfMangaBakaTag(
    val id: MangaBakaTagId,
    val contentRating: MangaBakaContentRating,
    val description: String?,
    val isSpoiler: Boolean?,
    val level: Int,
    val name: String,
    val namePath: String,
    val parentId: MangaBakaTagId?,
    val seriesCount: Int,
    val isGenre: Boolean,
    val mergedWith: Long?,
)
