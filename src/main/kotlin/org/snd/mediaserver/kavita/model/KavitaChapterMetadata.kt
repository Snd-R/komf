package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.model.DefaultAuthorRoles
import org.snd.mediaserver.model.MediaServerAuthor
import org.snd.mediaserver.model.MediaServerBookMetadata

@JsonClass(generateAdapter = true)
data class KavitaChapterMetadata(
    val id: Int,
    val chapterId: Int,
    val title: String,
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
    @AgeRating
    val ageRating: KavitaAgeRating,
    @PublicationStatus
    val publicationStatus: KavitaPublicationStatus,
//    val releaseDate: LocalDateTime, // not iso8601 format
    val summary: String?,
    val language: String?,
    val count: Int,
    val totalCount: Int,
    val wordCount: Int
)

fun KavitaChapterMetadata.mediaServerBookMetadata(chapter: KavitaChapter): MediaServerBookMetadata {
    val authors = writers.map { MediaServerAuthor(it.name, DefaultAuthorRoles.WRITER.name) } +
            coverArtists.map { MediaServerAuthor(it.name, DefaultAuthorRoles.COVER.name) } +
            pencillers.map { MediaServerAuthor(it.name, DefaultAuthorRoles.PENCILLER.name) } +
            letterers.map { MediaServerAuthor(it.name, DefaultAuthorRoles.LETTERER.name) } +
            inkers.map { MediaServerAuthor(it.name, DefaultAuthorRoles.INKER.name) } +
            colorists.map { MediaServerAuthor(it.name, DefaultAuthorRoles.COLORIST.name) } +
            editors.map { MediaServerAuthor(it.name, DefaultAuthorRoles.EDITOR.name) } +
            translators.map { MediaServerAuthor(it.name, DefaultAuthorRoles.TRANSLATOR.name) }

    return MediaServerBookMetadata(
        title = title,
        summary = summary,
        number = chapter.number ?: "0",
        numberSort = chapter.number,
//        releaseDate = releaseDate.toLocalDate(),
        releaseDate = null,
        authors = authors,
        tags = tags.map { it.title },
        isbn = null,
        links = emptyList(),

        titleLock = false,
        summaryLock = false,
        numberLock = false,
        numberSortLock = false,
        releaseDateLock = false,
        authorsLock = false,
        tagsLock = false,
        isbnLock = false,
        linksLock = false,
    )
}