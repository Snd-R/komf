package org.snd.metadata.providers.comicvine.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.snd.metadata.model.metadata.ProviderSeriesId

@JsonClass(generateAdapter = true)
data class ComicVineVolume(
    val id: Int,
    val name: String,
    @Json(name = "api_detail_url")
    val apiDetailUrl: String,
    @Json(name = "site_detail_url")
    val siteDetailUrl: String,

    val aliases: String?,
    @Json(name = "count_of_issues")
    val countOfIssues: Int?,
    val description: String?,
    val image: ComicVineImage?,
    val publisher: ComicVinePublisher?,
    @Json(name = "start_year")
    val startYear: String?,
    @Json(name = "resource_type")
    val resourceType: String?,
    val characters: List<ComicVineCredit>?,
    val locations: List<ComicVineCredit>?,
    val issues: List<ComicVineIssueSlim>?,
    val concepts: List<ComicVineConcept>?,
)

@JsonClass(generateAdapter = true)
data class ComicVineVolumeSearch(
    val id: Int,
    val name: String,
    @Json(name = "api_detail_url")
    val apiDetailUrl: String,
    @Json(name = "site_detail_url")
    val siteDetailUrl: String,

    val aliases: String?,
    @Json(name = "count_of_issues")
    val countOfIssues: Int?,
    val description: String?,
    @Json(name = "first_issue")
    val firstIssue: ComicVineIssueSlim?,
    @Json(name = "last_issue")
    val lastIssue: ComicVineIssueSlim?,
    val image: ComicVineImage?,
    val publisher: ComicVinePublisher?,
    @Json(name = "start_year")
    val startYear: String?,
    @Json(name = "resource_type")
    val resourceType: String?,
)

@JsonClass(generateAdapter = true)
data class ComicVineConcept(
    val id: Int,
    val name: String,
    @Json(name = "api_detail_url")
    val apiDetailUrl: String,
    @Json(name = "site_detail_url")
    val siteDetailUrl: String,
    val count: String,
)

@JsonClass(generateAdapter = true)
data class ComicVinePublisher(
    val id: Int,
    val name: String,
    @Json(name = "api_detail_url")
    val apiDetailUrl: String,
)

@JvmInline
value class ComicVineVolumeId(val id: Int)

fun ProviderSeriesId.toComicVineVolumeId() = ComicVineVolumeId(id.toInt())
