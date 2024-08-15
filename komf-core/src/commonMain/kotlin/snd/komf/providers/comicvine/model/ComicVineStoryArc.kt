package snd.komf.providers.comicvine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ComicVineStoryArcId(val value: Int) {
    override fun toString() = value.toString()
}

@Serializable
data class ComicVineStoryArc(
    val id: ComicVineStoryArcId,
    val name: String,
    val issues: List<ComicVineStoryArchIssue>,

    val aliases: String? = null,
    @SerialName("count_of_isssue_appearances")
    val countOfIssueAppearances: Int? = null,
    val deck: String? = null,
    val description: String? = null,
    val publisher: ComicVinePublisher? = null,
)

@Serializable
data class ComicVineStoryArchIssue(
    val id: ComicVineIssueId,
    val name: String,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
)