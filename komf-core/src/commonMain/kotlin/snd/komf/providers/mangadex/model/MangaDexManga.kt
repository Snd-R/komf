package snd.komf.providers.mangadex.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class MangaDexManga(
    val id: MangaDexMangaId,
    val type: String,
    val attributes: MangaDexAttributes,
    val relationships: List<MangaDexRelationship>
) {
    fun getCoverArt(): MangaDexCoverArt? = relationships.filterIsInstance<MangaDexCoverArt>().firstOrNull()

}

@JvmInline
@Serializable
value class MangaDexMangaId(val value: String)

@Serializable
data class MangaDexAttributes(
    val title: Map<String, String>,
    val altTitles: List<Map<String, String>>,
    val description: Map<String, String>,
    val isLocked: Boolean,
    val links: Map<String, String>?,
    val originalLanguage: String,
    val lastVolume: String?,
    val lastChapter: String?,
    val publicationDemographic: String?,
    val status: String,
    val year: Int?,
    val contentRating: String,
    val tags: List<MangaDexTag>,
    val state: String,
    val chapterNumbersResetOnNewVolume: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Int,
    val availableTranslatedLanguages: List<String?>? = null,
    val latestUploadedChapter: String?,
)

@Serializable
abstract class MangaDexRelationship {
    abstract val id: String
}

@Serializable
data class MangaDexUnknownRelationship(
    override val id: String,
    val type: String,
) : MangaDexRelationship()

@Serializable
@SerialName("author")
class MangaDexAuthor(
    override val id: String,
    val attributes: MangaDexAuthorAttributes
) : MangaDexRelationship()

@Serializable
@SerialName("artist")
class MangaDexArtist(
    override val id: String,
    val attributes: MangaDexAuthorAttributes
) : MangaDexRelationship()

@Serializable
data class MangaDexAuthorAttributes(
    val name: String
)

@Serializable
@SerialName("cover_art")
class MangaDexCoverArt(
    override val id: String,
    val attributes: MangaDexCoverArtAttributes
) : MangaDexRelationship()

@Serializable
data class MangaDexCoverArtAttributes(
    val fileName: String,
    val volume: String?,
    val locale: String?,
)

@Serializable
data class MangaDexTag(
    val id: String,
    val type: String,
    val attributes: MangaDexTagAttributes
)

@Serializable
data class MangaDexTagAttributes(
    val name: Map<String, String>,
    val description: Map<String, String>,
    val group: String,
    val version: Int,
)
