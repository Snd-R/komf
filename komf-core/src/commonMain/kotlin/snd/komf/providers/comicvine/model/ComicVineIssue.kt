package snd.komf.providers.comicvine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import snd.komf.model.ProviderBookId
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ComicVineIssueId(val value: Int) {
    override fun toString() = value.toString()
}

fun ProviderBookId.toComicVineIssueId() = ComicVineIssueId(id.toInt())

@Serializable
data class ComicVineIssue(
    val id: ComicVineIssueId,
    val name: String? = null,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String,

    val aliases: String? = null,
    @SerialName("associated_images")
    val associatedImages: List<ComicVineAltImage>? = null,
    @SerialName("character_credits")
    val characterCredits: List<ComicVineCredit>? = null,
    @SerialName("concept_credits")
    val conceptCredits: List<ComicVineCredit>? = null,
    @SerialName("cover_date")
    val coverDate: String? = null,
    @SerialName("date_added")
    val dateAdded: String? = null,
    @SerialName("date_last_updated")
    val dateLastUpdated: String? = null,
    val description: String? = null,
    val image: ComicVineImage? = null,
    @SerialName("issue_number")
    val issueNumber: String? = null,
    @SerialName("location_credits")
    val locationCredits: List<ComicVineCredit>? = null,
    @SerialName("object_credits")
    val objectCredits: List<ComicVineCredit>? = null,
    @SerialName("person_credits")
    val personCredits: List<ComicVinePersonCredit>? = null,
    @SerialName("store_date")
    val storeDate: String? = null,
    @SerialName("story_arc_credits")
    val storyArcCredits: List<ComicVineCredit>? = null,

    @SerialName("team_credits")
    val teamCredits: List<ComicVineCredit>? = null,
    val volume: ComicVineVolume? = null,
)

@Serializable
data class ComicVineIssueSlim(
    val id: Int,
    val name: String? = null,

    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String? = null,

    @SerialName("issue_number")
    val issueNumber: String? = null,
)

@Serializable
data class ComicVinePersonCredit(
    val id: Int,
    val name: String,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String,
    val role: String,
)

@Serializable
data class ComicVineAltImage(
    val id: Int,
    @SerialName("original_url")
    val originalUrl: String? = null,
    val caption: String? = null,
    @SerialName("image_tags")
    val imageTags: String? = null,
)

