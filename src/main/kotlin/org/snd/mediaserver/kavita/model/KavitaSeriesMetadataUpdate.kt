package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.kavita.model.KavitaAgeRating.UNKNOWN
import org.snd.mediaserver.kavita.model.KavitaPersonRole.*
import org.snd.mediaserver.kavita.model.KavitaPublicationStatus.ONGOING
import org.snd.mediaserver.model.MediaServerSeriesMetadataUpdate
import org.snd.metadata.model.AuthorRole
import org.snd.metadata.model.SeriesStatus

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
            .map { KavitaAuthor(id = 0, name = it, role = PUBLISHER) }

    val authors = authors?.groupBy { it.role }
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
        genres = genres?.map { KavitaGenre(id = 0, title = it) } ?: oldMeta.genres,
        tags = tags?.map { KavitaTag(id = 0, title = it) } ?: oldMeta.tags,
        writers = authors?.get(AuthorRole.WRITER.name)
            ?.map { KavitaAuthor(id = 0, name = it.name, role = WRITER) }
            ?.ifEmpty { oldMeta.writers } ?: oldMeta.writers,
        coverArtists = authors?.get(AuthorRole.COVER.name)
            ?.map { KavitaAuthor(id = 0, name = it.name, role = COVER_ARTIST) }
            ?.ifEmpty { oldMeta.coverArtists } ?: oldMeta.coverArtists,
        pencillers = authors?.get(AuthorRole.PENCILLER.name)
            ?.map { KavitaAuthor(id = 0, name = it.name, role = PENCILLER) }
            ?.ifEmpty { oldMeta.pencillers } ?: oldMeta.pencillers,
        inkers = authors?.get(AuthorRole.INKER.name)
            ?.map { KavitaAuthor(id = 0, name = it.name, role = INKER) }
            ?.ifEmpty { oldMeta.inkers } ?: oldMeta.inkers,
        colorists = authors?.get(AuthorRole.COLORIST.name)
            ?.map { KavitaAuthor(id = 0, name = it.name, role = COLORIST) }
            ?.ifEmpty { oldMeta.colorists } ?: oldMeta.colorists,
        letterers = authors?.get(AuthorRole.LETTERER.name)
            ?.map { KavitaAuthor(id = 0, name = it.name, role = LETTERER) }
            ?.ifEmpty { oldMeta.letterers } ?: oldMeta.letterers,
        editors = authors?.get(AuthorRole.EDITOR.name)
            ?.map { KavitaAuthor(id = 0, name = it.name, role = EDITOR) } ?: oldMeta.editors,
        translators = authors?.get(AuthorRole.TRANSLATOR.name)
            ?.map { KavitaAuthor(id = 0, name = it.name, role = TRANSLATOR) } ?: oldMeta.translators,
        ageRating = ageRating ?: oldMeta.ageRating
    )
    return KavitaSeriesMetadataUpdate(metadata, collectionTags)
}

fun kavitaSeriesResetRequest(seriesId: KavitaSeriesId): KavitaSeriesMetadataUpdate {
    val metadata = KavitaSeriesMetadata(
        id = 0,
        seriesId = seriesId.id,
        summary = "",
        collectionTags = emptyList(),
        genres = emptyList(),
        tags = emptyList(),
        writers = emptyList(),
        coverArtists = emptyList(),
        publishers = emptyList(),
        characters = emptyList(),
        pencillers = emptyList(),
        inkers = emptyList(),
        colorists = emptyList(),
        letterers = emptyList(),
        editors = emptyList(),
        translators = emptyList(),
        ageRating = UNKNOWN,
        releaseYear = 0,
        language = "",
        maxCount = 0,
        totalCount = 0,
        publicationStatus = ONGOING,

        languageLocked = false,
        summaryLocked = false,
        ageRatingLocked = false,
        publicationStatusLocked = false,
        genresLocked = false,
        tagsLocked = false,
        writersLocked = false,
        charactersLocked = false,
        coloristsLocked = false,
        editorsLocked = false,
        inkersLocked = false,
        letterersLocked = false,
        pencillersLocked = false,
        publishersLocked = false,
        translatorsLocked = false,
        coverArtistsLocked = false,
    )
    return KavitaSeriesMetadataUpdate(metadata, emptyList())
}











