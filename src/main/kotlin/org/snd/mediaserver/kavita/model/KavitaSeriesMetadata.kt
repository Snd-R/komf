package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.model.mediaserver.MediaServerAlternativeTitle
import org.snd.mediaserver.model.mediaserver.MediaServerAuthor
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesMetadata
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.AuthorRole.COLORIST
import org.snd.metadata.model.metadata.AuthorRole.COVER
import org.snd.metadata.model.metadata.AuthorRole.EDITOR
import org.snd.metadata.model.metadata.AuthorRole.INKER
import org.snd.metadata.model.metadata.AuthorRole.LETTERER
import org.snd.metadata.model.metadata.AuthorRole.PENCILLER
import org.snd.metadata.model.metadata.AuthorRole.TRANSLATOR
import org.snd.metadata.model.metadata.SeriesStatus
import org.snd.metadata.model.metadata.WebLink

@JsonClass(generateAdapter = true)
data class KavitaSeriesMetadata(
    val id: Int,
    val seriesId: Int,
    val summary: String?,
    val collectionTags: Collection<KavitaCollectionTag>,
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
    @AgeRating
    val ageRating: KavitaAgeRating,
    val releaseYear: Int,
    val language: String?,
    val maxCount: Int,
    val totalCount: Int,
    @PublicationStatus
    val publicationStatus: KavitaPublicationStatus,
    val webLinks: String?,

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

@JsonClass(generateAdapter = true)
data class KavitaCollectionTag(
    val id: Int,
    val title: String,
    val summary: String,
    val promoted: Boolean,
    val coverImage: String?,
    val coverImageLocked: Boolean,
)

@JsonClass(generateAdapter = true)
data class KavitaGenre(
    val id: Int,
    val title: String
)

@JsonClass(generateAdapter = true)
data class KavitaTag(
    val id: Int,
    val title: String
)

enum class KavitaPublicationStatus(val id: Int) {
    ONGOING(0),
    HIATUS(1),
    COMPLETED(2),
    CANCELLED(3),
    ENDED(4)
}

fun KavitaSeriesMetadata.mediaServerSeriesMetadata(series: KavitaSeries): MediaServerSeriesMetadata {
    val status = when (publicationStatus) {
        KavitaPublicationStatus.ONGOING -> SeriesStatus.ONGOING
        KavitaPublicationStatus.HIATUS -> SeriesStatus.HIATUS
        KavitaPublicationStatus.COMPLETED -> SeriesStatus.COMPLETED
        KavitaPublicationStatus.CANCELLED -> SeriesStatus.ABANDONED
        KavitaPublicationStatus.ENDED -> SeriesStatus.ENDED
    }
    val authors = writers.map { MediaServerAuthor(it.name, AuthorRole.WRITER.name) } +
            coverArtists.map { MediaServerAuthor(it.name, COVER.name) } +
            pencillers.map { MediaServerAuthor(it.name, PENCILLER.name) } +
            letterers.map { MediaServerAuthor(it.name, LETTERER.name) } +
            inkers.map { MediaServerAuthor(it.name, INKER.name) } +
            colorists.map { MediaServerAuthor(it.name, COLORIST.name) } +
            editors.map { MediaServerAuthor(it.name, EDITOR.name) } +
            translators.map { MediaServerAuthor(it.name, TRANSLATOR.name) }

    val authorsLock = sequenceOf(
        writerLocked,
        coverArtistLocked,
        pencillerLocked,
        lettererLocked,
        inkerLocked,
        coloristLocked,
        editorLocked,
        translatorLocked
    ).any { it } //TODO per role locks?

    return MediaServerSeriesMetadata(
        status = status,
        title = series.name,
        titleSort = series.sortName,
        alternativeTitles = series.localizedName?.let { listOf(MediaServerAlternativeTitle("Localized", it)) } ?: emptyList(),
        summary = summary ?: "",
        readingDirection = null,
        publisher = null,
        alternativePublishers = publishers.map { it.name }.toSet(),
        ageRating = ageRating.ageRating,
        language = language,
        genres = genres.map { it.title },
        tags = tags.map { it.title },
        totalBookCount = if (totalCount == 0) null else totalCount,
        authors = authors,
        releaseYear = releaseYear,
        links = webLinks?.split(",")?.map { WebLink(it, it) } ?: emptyList(),

        statusLock = publicationStatusLocked,
        titleLock = series.nameLocked,
        titleSortLock = series.sortNameLocked,
        summaryLock = summaryLocked,
        readingDirectionLock = false,
        publisherLock = publisherLocked,
        ageRatingLock = ageRatingLocked,
        languageLock = languageLocked,
        genresLock = genresLocked,
        tagsLock = tagsLocked,
        totalBookCountLock = false,
        authorsLock = authorsLock,
        releaseYearLock = releaseYearLocked,
        alternativeTitlesLock = series.localizedNameLocked,
        linksLock = false,
    )
}
