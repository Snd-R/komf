package org.snd.metadata.model

data class SeriesMetadata(
    val status: Status? = null,
    val title: String? = null,
    val titleSort: String? = null,
    val summary: String? = null,
    val publisher: String? = null,
    val readingDirection: ReadingDirection? = null,
    val ageRating: Int? = null,
    val language: String? = null,
    val genres: Collection<String> = emptyList(),
    val tags: Collection<String> = emptyList(),
    val totalBookCount: Int? = null,
    val authors: List<Author> = emptyList(),
    val alternativeTitles: Collection<String> = emptyList(),

    val thumbnail: Image? = null,
) {

    enum class Status {
        ENDED, ONGOING, ABANDONED, HIATUS
    }

    enum class ReadingDirection {
        LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL, WEBTOON
    }
}

