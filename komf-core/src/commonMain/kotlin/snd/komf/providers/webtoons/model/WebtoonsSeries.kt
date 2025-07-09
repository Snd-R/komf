package snd.komf.providers.webtoons.model

import io.ktor.http.*
import kotlinx.serialization.Serializable
import snd.komf.providers.webtoons.WebtoonsClient

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
    val status: Status,

    // These also sometimes have "other works", not parsing it for now
    val author: PersonInfo?,
    val adaptedBy: PersonInfo?,
    val artist: PersonInfo?,

    // Inconsistent formatting use of "." and "," as separators. Also uses shorthands
    val views: String,
    val subscribers: String,

    // There's a schedule of when it releases
    // val schedule: String,

    var chapters: Collection<Episode>?
)

@Serializable
enum class Status {
    ONGOING,
    COMPLETED,
    UNKNOWN
}

@Serializable
data class EpisodeListApiResponse(
    val result: EpisodeListResult,
    val success: Boolean
)

@Serializable
data class EpisodeListResult(
    val episodeList: List<Episode>,
    val nextCursor: Int
)

@Serializable
data class Episode(
    val episodeNo: Int,
    val thumbnail: String,
    val episodeTitle: String,
    val viewerLink: String,
    val exposureDateMillis: Long,
    val displayUp: Boolean,
    val hasBgm: Boolean = false
) {
    fun getUrl(): String {
        return "${WebtoonsClient.BASE_URL}$viewerLink"
    }

    fun getId(): WebtoonsChapterId {
        return WebtoonsChapterId(Url(getUrl()).encodedPathAndQuery)
    }
}


data class PersonInfo(val name: String, val description: String?) {
    constructor(name: String) : this(name, null)
}