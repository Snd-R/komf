package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.kavita.model.KavitaAgeRating.UNKNOWN
import org.snd.mediaserver.kavita.model.KavitaPersonRole.COLORIST
import org.snd.mediaserver.kavita.model.KavitaPersonRole.COVER_ARTIST
import org.snd.mediaserver.kavita.model.KavitaPersonRole.EDITOR
import org.snd.mediaserver.kavita.model.KavitaPersonRole.INKER
import org.snd.mediaserver.kavita.model.KavitaPersonRole.LETTERER
import org.snd.mediaserver.kavita.model.KavitaPersonRole.PENCILLER
import org.snd.mediaserver.kavita.model.KavitaPersonRole.PUBLISHER
import org.snd.mediaserver.kavita.model.KavitaPersonRole.TRANSLATOR
import org.snd.mediaserver.kavita.model.KavitaPersonRole.WRITER
import org.snd.mediaserver.kavita.model.KavitaPublicationStatus.ONGOING
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesMetadataUpdate
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.SeriesStatus

@JsonClass(generateAdapter = true)
data class KavitaSeriesMetadataUpdate(
    val seriesMetadata: KavitaSeriesMetadata,
    val collectionTags: Collection<KavitaCollectionTag>
)

fun MediaServerSeriesMetadataUpdate.kavitaSeriesMetadataUpdate(oldMeta: KavitaSeriesMetadata): KavitaSeriesMetadataUpdate {
    val status = when (status) {
        SeriesStatus.ENDED -> KavitaPublicationStatus.ENDED
        SeriesStatus.ONGOING -> ONGOING
        SeriesStatus.ABANDONED -> KavitaPublicationStatus.CANCELLED
        SeriesStatus.HIATUS -> KavitaPublicationStatus.HIATUS
        SeriesStatus.COMPLETED -> KavitaPublicationStatus.COMPLETED
        null -> null
    }
    val publishers =
        if (publisher == null && alternativePublishers == null) oldMeta.publishers
        else ((alternativePublishers ?: emptyList()) + listOfNotNull(publisher))
            .map { KavitaAuthor(id = 0, name = it, role = PUBLISHER) }.toSet()

    val authors = authors?.groupBy { it.role.lowercase() }
    val ageRating = ageRating
        ?.let { metadataRating ->
            KavitaAgeRating.values()
                .filter { it.ageRating != null }
                .sortedBy { it.ageRating }
                .firstOrNull { it.ageRating == it.ageRating!!.coerceAtLeast(metadataRating) }
                ?: KavitaAgeRating.ADULTS_ONLY
        }

    val collectionTags = oldMeta.collectionTags
    val metadata = oldMeta.copy(
        publicationStatus = status ?: oldMeta.publicationStatus,
        summary = summary ?: oldMeta.summary,
        publishers = publishers,
        genres = genres?.let { deduplicate(it) }?.map { KavitaGenre(id = 0, title = it) }?.toSet() ?: oldMeta.genres,
        tags = tags?.let { deduplicate(it) }?.map { KavitaTag(id = 0, title = it) }?.toSet() ?: oldMeta.tags,
        writers = authors
            ?.get(AuthorRole.WRITER.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = WRITER) }?.toSet()
            ?.ifEmpty { oldMeta.writers } ?: oldMeta.writers,
        coverArtists = authors
            ?.get(AuthorRole.COVER.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = COVER_ARTIST) }?.toSet()
            ?.ifEmpty { oldMeta.coverArtists } ?: oldMeta.coverArtists,
        pencillers = authors
            ?.get(AuthorRole.PENCILLER.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = PENCILLER) }?.toSet()
            ?.ifEmpty { oldMeta.pencillers } ?: oldMeta.pencillers,
        inkers = authors
            ?.get(AuthorRole.INKER.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = INKER) }?.toSet()
            ?.ifEmpty { oldMeta.inkers } ?: oldMeta.inkers,
        colorists = authors
            ?.get(AuthorRole.COLORIST.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = COLORIST) }?.toSet()
            ?.ifEmpty { oldMeta.colorists } ?: oldMeta.colorists,
        letterers = authors
            ?.get(AuthorRole.LETTERER.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = LETTERER) }?.toSet()
            ?.ifEmpty { oldMeta.letterers } ?: oldMeta.letterers,
        editors = authors
            ?.get(AuthorRole.EDITOR.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = EDITOR) }?.toSet() ?: oldMeta.editors,
        translators = authors
            ?.get(AuthorRole.TRANSLATOR.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = TRANSLATOR) }?.toSet() ?: oldMeta.translators,
        ageRating = ageRating ?: oldMeta.ageRating,
        language = language ?: oldMeta.language,
        releaseYear = releaseYear ?: oldMeta.releaseYear,
        webLinks = links?.map { it.url }?.joinToString(separator = ",") ?: oldMeta.webLinks
    )
    return KavitaSeriesMetadataUpdate(metadata, collectionTags)
}

private val normalizeRegex = "[^\\p{L}0-9+!]".toRegex()
private fun deduplicate(values: Collection<String>) = values
    .map { normalizeRegex.replace(it, "").trim().lowercase() to it }
    .distinctBy { (normalized, _) -> normalized }
    .map { (_, value) -> value }

fun kavitaSeriesResetRequest(seriesId: KavitaSeriesId): KavitaSeriesMetadataUpdate {
    val metadata = KavitaSeriesMetadata(
        id = 0,
        seriesId = seriesId.id,
        summary = "",
        collectionTags = emptyList(),
        genres = emptySet(),
        tags = emptySet(),
        writers = emptySet(),
        coverArtists = emptySet(),
        publishers = emptySet(),
        characters = emptySet(),
        pencillers = emptySet(),
        inkers = emptySet(),
        colorists = emptySet(),
        letterers = emptySet(),
        editors = emptySet(),
        translators = emptySet(),
        ageRating = UNKNOWN,
        releaseYear = 0,
        language = "",
        maxCount = 0,
        totalCount = 0,
        publicationStatus = ONGOING,
        webLinks = "",

        languageLocked = false,
        summaryLocked = false,
        ageRatingLocked = false,
        publicationStatusLocked = false,
        genresLocked = false,
        tagsLocked = false,
        writerLocked = false,
        characterLocked = false,
        coloristLocked = false,
        editorLocked = false,
        inkerLocked = false,
        lettererLocked = false,
        pencillerLocked = false,
        publisherLocked = false,
        translatorLocked = false,
        coverArtistLocked = false,
        releaseYearLocked = false,
    )
    return KavitaSeriesMetadataUpdate(metadata, emptyList())
}











