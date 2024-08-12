package snd.komf.providers.kodansha.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class KodanshaSeries(
    val id: Int,
    val title: String,
    val genres: List<KodanshaGenre>?,
    val creators: List<KodanshaCreator>?,
    val completionStatus: String?,
    val description: String?,
    val ageRating: String?,
    val thumbnails: List<KodanshaThumbnail>?,
    val publisher: String?,
    val readableUrl: String?,
)

@JvmInline
value class KodanshaSeriesId(val id: Int)

@Serializable
data class KodanshaGenre(
    val name: String,
    val id: Int
)
