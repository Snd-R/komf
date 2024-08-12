package snd.komf.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.floor

@JvmInline
@Serializable
value class ProviderBookId(val id: String)

@Serializable
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

@Serializable
data class Chapter(
    val name: String?,
    val number: Int
)

@Serializable
data class BookRange(
    val start: Double,
    val end: Double
) {
    constructor(start: Double) : this(start, start)

    constructor(start: Int) : this(start.toDouble(), start.toDouble())

    override fun toString(): String {
        val start = if (floor(start) == start) start.toInt() else start
        val end = if (floor(end) == end) end.toInt() else end
        return if (start == end) {
            start.toString()
        } else "$start-$end"
    }
}

@Serializable
data class ProviderBookMetadata(
    val id: ProviderBookId? = null,
    val seriesId: ProviderSeriesId? = null,
    val metadata: BookMetadata,
)

