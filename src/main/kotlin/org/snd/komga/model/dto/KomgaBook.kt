package org.snd.komga.model.dto

import com.squareup.moshi.JsonClass
import java.time.LocalDate
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class KomgaBook(
    val id: String,
    val seriesId: String,
    val seriesTitle: String,
    val libraryId: String,
    val name: String,
    val url: String,
    val number: Int,
    val created: LocalDateTime,
    val lastModified: LocalDateTime,
    val fileLastModified: LocalDateTime,
    val sizeBytes: Long,
    val size: String,
    val media: Media,
    val metadata: KomgaBookMetadata,
    val readProgress: ReadProgress?,
    val deleted: Boolean,
    val fileHash: String,
) {
    fun bookId(): KomgaBookId = KomgaBookId(id)
}

@JsonClass(generateAdapter = true)
data class KomgaBookMetadata(
    val title: String,
    val summary: String,
    val number: String,
    val numberSort: Float,
    val releaseDate: LocalDate?,
    val authors: List<KomgaAuthor>,
    val tags: Set<String>,
    val isbn: String,
    val links: List<WebLink>,

    val titleLock: Boolean,
    val summaryLock: Boolean,
    val numberLock: Boolean,
    val numberSortLock: Boolean,
    val releaseDateLock: Boolean,
    val authorsLock: Boolean,
    val tagsLock: Boolean,
    val isbnLock: Boolean,
    val linksLock: Boolean,

    val created: LocalDateTime,
    val lastModified: LocalDateTime,
)

@JsonClass(generateAdapter = true)
data class Media(
    val status: String,
    val mediaType: String,
    val pagesCount: Int,
    val comment: String,
)

@JsonClass(generateAdapter = true)
data class ReadProgress(
    val page: Int,
    val completed: Boolean,
    val readDate: LocalDateTime,
    val created: LocalDateTime,
    val lastModified: LocalDateTime,
)
