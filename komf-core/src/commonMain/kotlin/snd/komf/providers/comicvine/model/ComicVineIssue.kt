package snd.komf.providers.comicvine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import snd.komf.model.ProviderBookId
import kotlin.jvm.JvmInline

@Serializable
data class ComicVineIssue(
    val id: Int,
    val name: String?,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String,

    val aliases: String?,
    @SerialName("associated_images")
    val associatedImages: List<ComicVineAltImage>?,
    @SerialName("character_credits")
    val characterCredits: List<ComicVineCredit>?,
    @SerialName("concept_credits")
    val conceptCredits: List<ComicVineCredit>?,
    @SerialName("cover_date")
    val coverDate: String?,
    @SerialName("date_added")
    val dateAdded: String?,
    @SerialName("date_last_updated")
    val dateLastUpdated: String?,
    val description: String?,
    val image: ComicVineImage?,
    @SerialName("issue_number")
    val issueNumber: String?,
    @SerialName("location_credits")
    val locationCredits: List<ComicVineCredit>?,
    @SerialName("object_credits")
    val objectCredits: List<ComicVineCredit>?,
    @SerialName("person_credits")
    val personCredits: List<ComicVinePersonCredit>?,
    @SerialName("store_date")
    val storeDate: String?,
    @SerialName("story_arc_credits")
    val storyArcCredits: List<ComicVineCredit>?,

    @SerialName("team_credits")
    val teamCredits: List<ComicVineCredit>?,
    val volume: ComicVineVolume?,
)

@Serializable
data class ComicVineIssueSlim(
    val id: Int,
    val name: String?,

    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String?,

    @SerialName("issue_number")
    val issueNumber: String?,
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
    val originalUrl: String?,
    val caption: String?,
    @SerialName("image_tags")
    val imageTags: String?,
)

@JvmInline
value class ComicVineIssueId(val id: Int)

fun ProviderBookId.toComicVineIssueId() = ComicVineIssueId(id.toInt())
