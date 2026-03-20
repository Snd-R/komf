package snd.komf.mediaserver.kavita.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import snd.komf.mediaserver.model.MediaServerBookId

@JvmInline
@Serializable
value class KavitaChapterId(val value: Int) {
    override fun toString() = value.toString()
}

fun MediaServerBookId.toKavitaChapterId() = KavitaChapterId(value.toInt())

@Serializable
data class KavitaChapter(
    val id: KavitaChapterId,
    val range: String? = null,
    val number: String? = null,
    val minNumber: Double,
    val maxNumber: Double,
    val sortOrder: Double,
    val pages: Int,
    val isSpecial: Boolean,
    val title: String,
    val files: Collection<KavitaChapterFile>,
    val pagesRead: Int,
    val coverImageLocked: Boolean,
    val volumeId: KavitaVolumeId,
    val createdUtc: LocalDateTime,
    val releaseDate: LocalDateTime,

    val titleName: String,
    val summary: String? = null,
    val ageRating: KavitaAgeRating,
    val webLinks: String,
    val isbn: String,

    val writers: Collection<KavitaAuthor>,
    val coverArtists: Collection<KavitaAuthor>,
    val publishers: Collection<KavitaAuthor>,
    val characters: Collection<KavitaAuthor>,
    val pencillers: Collection<KavitaAuthor>,
    val inkers: Collection<KavitaAuthor>,
    val imprints: Collection<KavitaAuthor>,
    val colorists: Collection<KavitaAuthor>,
    val letterers: Collection<KavitaAuthor>,
    val editors: Collection<KavitaAuthor>,
    val translators: Collection<KavitaAuthor>,
    val teams: Collection<KavitaAuthor>,
    val locations: Collection<KavitaAuthor>,

    val genres: Collection<KavitaGenre>,
    val tags: Collection<KavitaTag>,
    val language: String? = null,
    val count: Int,
    val totalCount: Int,

    val languageLocked: Boolean,
    val summaryLocked: Boolean,
    val ageRatingLocked: Boolean,
    val genresLocked: Boolean,
    val tagsLocked: Boolean,
    val writerLocked: Boolean,
    val characterLocked: Boolean,
    val coloristLocked: Boolean,
    val editorLocked: Boolean,
    val inkerLocked: Boolean,
    val imprintLocked: Boolean,
    val lettererLocked: Boolean,
    val pencillerLocked: Boolean,
    val publisherLocked: Boolean,
    val translatorLocked: Boolean,
    val teamLocked: Boolean,
    val locationLocked: Boolean,
    val coverArtistLocked: Boolean,
    val releaseDateLocked: Boolean,
    val titleNameLocked: Boolean,
    val sortOrderLocked: Boolean,
//    val isbnLocked: Boolean,
)

@Serializable
data class KavitaChapterFile(
    val id: Int,
    val filePath: String,
    val pages: Int,
    val format: Int,
    val created: LocalDateTime
)
