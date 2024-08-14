package snd.komf.providers.comicvine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable()
data class ComicVineImage(
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("medium_url")
    val mediumUrl: String? = null,
    @SerialName("screen_url")
    val screenUrl: String? = null,
    @SerialName("screen_large_url")
    val screenLargeUrl: String? = null,
    @SerialName("small_url")
    val smallUrl: String? = null,
    @SerialName("super_url")
    val superUrl: String? = null,
    @SerialName("thumb_url")
    val thumbUrl: String? = null,
    @SerialName("tiny_url")
    val tinyUrl: String? = null,
    @SerialName("original_url")
    val originalUrl: String? = null,
    @SerialName("image_tags")
    val imageTags: String? = null,
)

