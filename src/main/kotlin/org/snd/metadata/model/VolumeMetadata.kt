package org.snd.metadata.model

import org.snd.komga.model.dto.AuthorUpdate
import org.snd.komga.model.dto.BookMetadataUpdate
import org.snd.komga.model.dto.WebLinkUpdate
import java.time.LocalDate


data class VolumeMetadata(
    val title: String? = null,
    val summary: String? = null,
    val number: Int? = null,
    val numberSort: Float? = null,
    val releaseDate: LocalDate? = null,
    val authors: List<Author>? = null,
    val tags: Set<String>? = null,
    val isbn: String? = null,
    val links: List<WebLink>? = null,
    val chapters: Collection<Chapter>? = null,

    val startChapter: Int?,
    val endChapter: Int?,

    val thumbnail: Thumbnail? = null,
)

fun VolumeMetadata.toBookMetadataUpdate() = BookMetadataUpdate(
    title = title,
    summary = summary,
    releaseDate = releaseDate,
    authors = authors?.map { AuthorUpdate(it.name, it.role) },
    tags = tags,
    isbn = isbn,
    links = links?.map { WebLinkUpdate(it.label, it.url) }
)

data class Chapter(
    val name: String?,
    val number: Int
)

data class WebLink(
    val label: String,
    val url: String,
)
