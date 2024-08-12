package snd.komf.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ProviderSeriesId(val value: String){
    override fun toString() = value
}

@Serializable
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

@Serializable
data class SeriesTitle(
    val name: String,
    val type: TitleType?,
    val language: String?,
)

@Serializable
data class ProviderSeriesMetadata(
    val id: ProviderSeriesId,
    val metadata: SeriesMetadata,
    val books: List<SeriesBook> = emptyList(),
)

@Serializable
data class SeriesBook(
    val id: ProviderBookId,
    val number: BookRange?,
    val name: String?,
    val type: String?,
    val edition: String?
)

@Serializable
data class ReleaseDate(
    val year: Int?,
    val month: Int?,
    val day: Int?
)

enum class TitleType(val label: String) {
    ROMAJI("Romaji"),
    LOCALIZED("Localized"),
    NATIVE("Native"),
}
enum class ReadingDirection {
    LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL, WEBTOON
}
enum class SeriesStatus {
    ENDED,
    ONGOING,
    ABANDONED,
    HIATUS,
    COMPLETED
}


fun LocalDate.toReleaseDate() = ReleaseDate(year = year, month = monthNumber, day = dayOfMonth)

