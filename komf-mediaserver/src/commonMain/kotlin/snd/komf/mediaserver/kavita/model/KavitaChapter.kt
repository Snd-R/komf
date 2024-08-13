package snd.komf.mediaserver.kavita.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import snd.komf.mediaserver.model.MediaServerBookId

@JvmInline
@Serializable
value class KavitaChapterId(val value: Int)

fun MediaServerBookId.toKavitaChapterId() = KavitaChapterId(value.toInt())

@Serializable
data class KavitaChapter(
    val id: KavitaChapterId,
    val range: String? = null,
    val number: String? = null,
    val pages: Int,
    val isSpecial: Boolean,
    val title: String,
    val files: Collection<KavitaChapterFile>,
    val pagesRead: Int,
    val coverImageLocked: Boolean,
    val volumeId: KavitaVolumeId,
    val createdUtc: LocalDateTime,

    val titleName: String,
    val summary: String? = null,
    val ageRating: KavitaAgeRating,
    val isbn: String,

    val writers: Collection<KavitaAuthor>,
    val coverArtists: Collection<KavitaAuthor>,
    val publishers: Collection<KavitaAuthor>,
    val characters: Collection<KavitaAuthor>,
    val pencillers: Collection<KavitaAuthor>,
    val inkers: Collection<KavitaAuthor>,
    val colorists: Collection<KavitaAuthor>,
    val letterers: Collection<KavitaAuthor>,
    val editors: Collection<KavitaAuthor>,
    val translators: Collection<KavitaAuthor>,

    val genres: Collection<KavitaGenre>,
    val tags: Collection<KavitaTag>,
    val publicationStatus: KavitaPublicationStatus,
    val releaseDate: LocalDateTime,
    val language: String? = null,
    val count: Int,
    val totalCount: Int,
)

@Serializable
data class KavitaChapterFile(
    val id: Int,
    val filePath: String,
    val pages: String,
    val format: Int,
    val created: LocalDateTime
)

