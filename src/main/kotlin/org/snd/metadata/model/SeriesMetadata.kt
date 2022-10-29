package org.snd.metadata.model

data class SeriesMetadata(
    val status: SeriesStatus? = null,
    val title: String? = null,
    val titleSort: String? = null,
    val summary: String? = null,
    val publisher: String? = null,
    val alternativePublishers: Collection<String> = emptyList(),
    val readingDirection: ReadingDirection? = null,
    val ageRating: Int? = null,
    val language: String? = null,
    val genres: Collection<String> = emptyList(),
    val tags: Collection<String> = emptyList(),
    val totalBookCount: Int? = null,
    val authors: List<Author> = emptyList(),
    val alternativeTitles: Collection<String> = emptyList(),
    val releaseYear: Int? = null,

    val thumbnail: Image? = null,
)

