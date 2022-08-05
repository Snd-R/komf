package org.snd.metadata.model

import org.snd.metadata.Provider

data class SeriesMetadata(
    val id: ProviderSeriesId,
    val provider: Provider,

    val status: Status? = null,
    val title: String? = null,
    val titleSort: String? = null,
    val summary: String? = null,
    val publisher: String? = null,
    val readingDirection: ReadingDirection? = null,
    val ageRating: Int? = null,
    val language: String? = null,
    val genres: Collection<String>? = null,
    val tags: Collection<String>? = null,
    val totalBookCount: Int? = null,
    val authors: List<Author>? = null,

    val thumbnail: Thumbnail? = null,

    val books: List<SeriesBook> = emptyList()

) {

    enum class Status {
        ENDED, ONGOING, ABANDONED, HIATUS
    }

    enum class ReadingDirection {
        LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL, WEBTOON
    }
}

data class SeriesBook(
    val id: ProviderBookId,
    val number: Int?,
    val name: String?,
    val type: String?,
    val edition: String?
)
