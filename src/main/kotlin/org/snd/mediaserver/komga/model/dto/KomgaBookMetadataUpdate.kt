package org.snd.mediaserver.komga.model.dto

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.model.MediaServerBookMetadataUpdate
import java.time.LocalDate


@JsonClass(generateAdapter = true)
data class KomgaBookMetadataUpdate(
    val title: String? = null,
    val summary: String? = null,
    val number: String? = null,
    val numberSort: Double? = null,
    val releaseDate: LocalDate? = null,
    val authors: List<KomgaAuthor>? = null,
    val tags: Collection<String>? = null,
    val isbn: String? = null,
    val links: List<KomgaWebLink>? = null,

    val titleLock: Boolean? = null,
    val summaryLock: Boolean? = null,
    val numberLock: Boolean? = null,
    val numberSortLock: Boolean? = null,
    val releaseDateLock: Boolean? = null,
    val authorsLock: Boolean? = null,
    val tagsLock: Boolean? = null,
    val isbnLock: Boolean? = null,
    val linksLock: Boolean? = null,
)

fun MediaServerBookMetadataUpdate.komgaMetadataUpdate() = KomgaBookMetadataUpdate(
    title = title,
    summary = summary,
    number = number,
    numberSort = numberSort,
    releaseDate = releaseDate,
    authors = authors?.map { KomgaAuthor(it.name, it.role) },
    tags = tags,
    isbn = isbn,

    titleLock = titleLock,
    summaryLock = summaryLock,
    numberLock = numberLock,
    numberSortLock = numberSortLock,
    releaseDateLock = releaseDateLock,
    authorsLock = authorsLock,
    tagsLock = tagsLock,
    isbnLock = isbnLock,
    linksLock = linksLock,
)

fun bookMetadataResetRequest(name: String) = KomgaBookMetadataUpdate(
    title = name,
    summary = "",
    releaseDate = null,
    authors = emptyList(),
    tags = emptySet(),
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
    linksLock = false
)
