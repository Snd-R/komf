package snd.komf.providers.kodansha.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class KodanshaSeries(
    val id: Int,
    val title: String,
    val genres: List<KodanshaGenre>? = null,
    val creators: List<KodanshaCreator>? = null,
    val completionStatus: String? = null,
    val description: String? = null,
    val ageRating: String? = null,
    val thumbnails: List<KodanshaThumbnail>? = null,
    val publisher: String? = null,
    val readableUrl: String? = null,
)

@JvmInline
value class KodanshaSeriesId(val id: Int)

@Serializable
data class KodanshaGenre(
    val name: String,
    val id: Int
)
