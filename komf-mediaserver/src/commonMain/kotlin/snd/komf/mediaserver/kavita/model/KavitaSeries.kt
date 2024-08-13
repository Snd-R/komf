package snd.komf.mediaserver.kavita.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import snd.komf.mediaserver.model.MediaServerSeriesId

@JvmInline
@Serializable
value class KavitaSeriesId(val value: Int)

fun MediaServerSeriesId.toKavitaSeriesId() = KavitaSeriesId(value.toInt())

@Serializable
class KavitaSeries(
    val id: KavitaSeriesId,
    val name: String,
    val libraryId: KavitaLibraryId,
    val libraryName: String,
    val originalName: String,
    val localizedName: String? = null,
    val sortName: String,
    val summary: String? = null,
    val pages: Int,
    val format: Int,
    val created: LocalDateTime,
    val folderPath: String,
    val coverImageLocked: Boolean,
    val localizedNameLocked: Boolean,
    val nameLocked: Boolean,
    val sortNameLocked: Boolean,
)


@Serializable
data class KavitaSeriesDetails(
    val totalCount: Int,
    val chapters: Collection<KavitaChapter>? = null,
    val storylineChapters: Collection<KavitaChapter>? = null,
    val unreadCount: Int? = null,
    val volumes: Collection<KavitaVolume>? = null,
)

@Serializable
data class KavitaSeriesMetadata(
    val id: Int,
    val seriesId: KavitaSeriesId,
    val summary: String? = null,
    val genres: Set<KavitaGenre>,
    val tags: Set<KavitaTag>,
    val writers: Set<KavitaAuthor>,
    val coverArtists: Set<KavitaAuthor>,
    val publishers: Set<KavitaAuthor>,
    val characters: Set<KavitaAuthor>,
    val pencillers: Set<KavitaAuthor>,
    val inkers: Set<KavitaAuthor>,
    val colorists: Set<KavitaAuthor>,
    val letterers: Set<KavitaAuthor>,
    val editors: Set<KavitaAuthor>,
    val translators: Set<KavitaAuthor>,
    val ageRating: KavitaAgeRating,
    val releaseYear: Int,
    val language: String? = null,
    val maxCount: Int,
    val totalCount: Int,
    val publicationStatus: KavitaPublicationStatus,
    val webLinks: String? = null,

    val languageLocked: Boolean,
    val summaryLocked: Boolean,
    val ageRatingLocked: Boolean,
    val publicationStatusLocked: Boolean,
    val genresLocked: Boolean,
    val tagsLocked: Boolean,
    val writerLocked: Boolean,
    val characterLocked: Boolean,
    val coloristLocked: Boolean,
    val editorLocked: Boolean,
    val inkerLocked: Boolean,
    val lettererLocked: Boolean,
    val pencillerLocked: Boolean,
    val publisherLocked: Boolean,
    val translatorLocked: Boolean,
    val coverArtistLocked: Boolean,
    val releaseYearLocked: Boolean,
)

@Serializable
data class KavitaGenre(
    val id: Int,
    val title: String
)

@Serializable
data class KavitaTag(
    val id: Int,
    val title: String
)

@Serializable(with = KavitaPublicationStatusSerializer::class)
enum class KavitaPublicationStatus(val id: Int) {
    ONGOING(0),
    HIATUS(1),
    COMPLETED(2),
    CANCELLED(3),
    ENDED(4)
}

class KavitaPublicationStatusSerializer : KSerializer<KavitaPublicationStatus> {
    override val descriptor = PrimitiveSerialDescriptor("KavitaPublicationStatus", PrimitiveKind.INT).nullable
    override fun serialize(encoder: Encoder, value: KavitaPublicationStatus) = encoder.encodeInt(value.id)
    override fun deserialize(decoder: Decoder): KavitaPublicationStatus = when (decoder.decodeInt()) {
        0 -> KavitaPublicationStatus.ONGOING
        1 -> KavitaPublicationStatus.HIATUS
        2 -> KavitaPublicationStatus.COMPLETED
        3 -> KavitaPublicationStatus.CANCELLED
        4 -> KavitaPublicationStatus.ENDED
        else -> error("Unsupported status code")
    }
}

