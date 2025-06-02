package snd.komf.providers.webtoons.model

import kotlinx.datetime.LocalDate

@JvmInline
value class WebtoonsSeriesId(val value: String)

@JvmInline
value class WebtoonsChapterId(val value: String)

data class WebtoonsSeries(
    val id: WebtoonsSeriesId,
    val title: String,
    val description: String?,
    val url: String,
    val thumbnailUrl: String?,

    val genres: Collection<String>,

    // These also sometimes have "other works", not parsing it for now
    val author: PersonInfo?,
    val adaptedBy: PersonInfo?,
    val artist: PersonInfo?,

    // Inconsistent formatting use of "." and "," as separators. Also uses shorthands
    val views: String,
    val subscribers: String,
    val score: Double,

    // There's a schedule of when it releases
    // val schedule: String,

    var chapters: Collection<WebtoonsChapter>?
)

data class WebtoonsChapter(
    val id: WebtoonsChapterId,
    val title: String,
    val url: String,
    val number: Double,
    val thumbnailUrl: String,
    val releaseDate: LocalDate,
    val likes: String,
)

data class PersonInfo(val name: String, val description: String?) {
    constructor(name: String) : this(name, null)
}