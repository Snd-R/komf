package snd.komf.mediaserver.kavita.model.request

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import snd.komf.mediaserver.kavita.model.KavitaAgeRating
import snd.komf.mediaserver.kavita.model.KavitaAuthor
import snd.komf.mediaserver.kavita.model.KavitaChapterId
import snd.komf.mediaserver.kavita.model.KavitaGenre
import snd.komf.mediaserver.kavita.model.KavitaTag

@Serializable
data class KavitaChapterMetadataUpdateRequest(
    val id: KavitaChapterId,

    val summary: String? = null,
    val genres: Collection<KavitaGenre>,
    val tags: Collection<KavitaTag>,
    val ageRating: KavitaAgeRating,
    val language: String? = null,
    val weblinks: String,
    val isbn: String,
    val releaseDate: LocalDateTime,
    val titleName: String,
    val sortOrder: Double,

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

    val ageRatingLocked: Boolean,
    val titleNameLocked: Boolean,
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
    val languageLocked: Boolean,
    val summaryLocked: Boolean,
    val isbnLocked: Boolean,
    val releaseDateLocked: Boolean,
    val sortOrderLocked: Boolean,
)