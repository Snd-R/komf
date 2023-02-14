package org.snd.metadata.model.metadata

import com.squareup.moshi.JsonClass
import org.snd.metadata.model.Image
import java.time.LocalDate


@JsonClass(generateAdapter = true)
data class BookMetadata(
    val title: String? = null,
    val summary: String? = null,
    val number: BookRange? = null,
    val numberSort: Double? = null,
    val releaseDate: LocalDate? = null,
    val authors: List<Author> = emptyList(),
    val tags: Set<String> = emptySet(),
    val isbn: String? = null,
    val links: List<WebLink> = emptyList(),
    val chapters: Collection<Chapter> = emptyList(),

    val startChapter: Int? = null,
    val endChapter: Int? = null,

    val thumbnail: Image? = null,
)

@JsonClass(generateAdapter = true)
data class Chapter(
    val name: String?,
    val number: Int
)
