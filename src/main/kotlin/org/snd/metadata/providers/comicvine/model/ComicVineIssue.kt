package org.snd.metadata.providers.comicvine.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.snd.metadata.model.metadata.ProviderBookId

@JsonClass(generateAdapter = true)
data class ComicVineIssue(
    val id: Int,
    val name: String?,
    @Json(name = "api_detail_url")
    val apiDetailUrl: String,
    @Json(name = "site_detail_url")
    val siteDetailUrl: String,

    val aliases: String?,
    @Json(name = "associated_images")
    val associatedImages: List<ComicVineAltImage>?,
    @Json(name = "character_credits")
    val characterCredits: List<ComicVineCredit>?,
    @Json(name = "concept_credits")
    val conceptCredits: List<ComicVineCredit>?,
    @Json(name = "cover_date")
    val coverDate: String?,
    @Json(name = "date_added")
    val dateAdded: String?,
    @Json(name = "date_last_updated")
    val dateLastUpdated: String?,
    val description: String?,
    val image: ComicVineImage?,
    @Json(name = "issue_number")
    val issueNumber: String?,
    @Json(name = "location_credits")
    val locationCredits: List<ComicVineCredit>?,
    @Json(name = "object_credits")
    val objectCredits: List<ComicVineCredit>?,
    @Json(name = "person_credits")
    val personCredits: List<ComicVinePersonCredit>?,
    @Json(name = "store_date")
    val storeDate: String?,
    @Json(name = "story_arc_credits")
    val storyArcCredits: List<ComicVineCredit>?,

    @Json(name = "team_credits")
    val teamCredits: List<ComicVineCredit>?,
    val volume: ComicVineVolume?,
)

@JsonClass(generateAdapter = true)
data class ComicVineIssueSlim(
    val id: Int,
    val name: String?,

    @Json(name = "api_detail_url")
    val apiDetailUrl: String,
    @Json(name = "site_detail_url")
    val siteDetailUrl: String?,

    @Json(name = "issue_number")
    val issueNumber: String?,
)

@JsonClass(generateAdapter = true)
data class ComicVinePersonCredit(
    val id: Int,
    val name: String,
    @Json(name = "api_detail_url")
    val apiDetailUrl: String,
    @Json(name = "site_detail_url")
    val siteDetailUrl: String,
    val role: String,
)

@JsonClass(generateAdapter = true)
data class ComicVineAltImage(
    val id: Int,
    @Json(name = "original_url")
    val originalUrl: String?,
    val caption: String?,
    @Json(name = "image_tags")
    val imageTags: String?,
)

@JvmInline
value class ComicVineIssueId(val id: Int)

fun ProviderBookId.toComicVineIssueId() = ComicVineIssueId(id.toInt())
