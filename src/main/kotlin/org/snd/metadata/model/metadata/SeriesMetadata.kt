package org.snd.metadata.model.metadata

import com.squareup.moshi.JsonClass
import org.snd.metadata.model.Image
import java.time.LocalDate

@JsonClass(generateAdapter = true)
data class SeriesMetadata(
    val status: SeriesStatus? = null,
    val title: SeriesTitle? = null,
    val titles: Collection<SeriesTitle> = emptyList(),
    val summary: String? = null,
    val publisher: String? = null,
    val alternativePublishers: Set<String> = emptySet(),
    val readingDirection: ReadingDirection? = null,
    val ageRating: Int? = null,
    val language: String? = null,
    val genres: Collection<String> = emptyList(),
    val tags: Collection<String> = emptyList(),
    val totalBookCount: Int? = null,
    val authors: List<Author> = emptyList(),
    val releaseDate: ReleaseDate? = null,
    val links: Collection<WebLink> = emptyList(),
    val score: Double? = null,

    val thumbnail: Image? = null,
)

@JsonClass(generateAdapter = true)
data class ReleaseDate(
    val year: Int?,
    val month: Int?,
    val day: Int?
)

fun LocalDate.toReleaseDate() = ReleaseDate(year = year, month = monthValue, day = dayOfMonth)