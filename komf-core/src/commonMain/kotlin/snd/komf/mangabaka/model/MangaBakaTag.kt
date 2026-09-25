package snd.komf.mangabaka.model

data class MangaBakaTag(
    val id: MangaBakaTagId,
    val parentId: MangaBakaTagId? = null,
    val mergedWith: Long? = null,
    val name: String,
    val namePath: String,
    val description: String? = null,
    val isSpoiler: Boolean = false,
    val isGenre: Boolean = false,
    val contentRating: MangaBakaContentRating,
    val seriesCount: Int,
    val level: Int,
)