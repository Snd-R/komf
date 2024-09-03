package snd.komf.providers.hentag

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
internal data class HentagBook(
    val title: String,
    val coverImageUrl: String? = null,
    val parodies: List<String>? = null,
    val circles: List<String>? = null,
    val artists: List<String>? = null,
    val characters: List<String>? = null,
    val maleTags: List<String>? = null,
    val femaleTags: List<String>? = null,
    val otherTags: List<String>? = null,
    val language: String,
    val category: String,
    val createdAt: Instant,
    val lastModified: Instant,
    val publishedOn: Instant?,
    val locations: List<String>? = null,
    val favorite: Boolean,
)