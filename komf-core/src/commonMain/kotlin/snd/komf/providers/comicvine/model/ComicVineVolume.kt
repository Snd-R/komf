package snd.komf.providers.comicvine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import snd.komf.model.ProviderSeriesId
import kotlin.jvm.JvmInline

@Serializable
data class ComicVineVolume(
    val id: Int,
    val name: String,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String,

    val aliases: String?,
    @SerialName("count_of_issues")
    val countOfIssues: Int?,
    val description: String?,
    val image: ComicVineImage?,
    val publisher: ComicVinePublisher?,
    @SerialName("start_year")
    val startYear: String?,
    @SerialName("resource_type")
    val resourceType: String?,
    val characters: List<ComicVineCredit>?,
    val locations: List<ComicVineCredit>?,
    val issues: List<ComicVineIssueSlim>?,
    val concepts: List<ComicVineConcept>?,
)

@Serializable
data class ComicVineVolumeSearch(
    val id: Int,
    val name: String,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String,

    val aliases: String?,
    @SerialName("count_of_issues")
    val countOfIssues: Int?,
    val description: String?,
    @SerialName("first_issue")
    val firstIssue: ComicVineIssueSlim?,
    @SerialName("last_issue")
    val lastIssue: ComicVineIssueSlim?,
    val image: ComicVineImage?,
    val publisher: ComicVinePublisher?,
    @SerialName("start_year")
    val startYear: String?,
    @SerialName("resource_type")
    val resourceType: String?,
)

@Serializable
data class ComicVineConcept(
    val id: Int,
    val name: String,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String,
    val count: String,
)

@Serializable
data class ComicVinePublisher(
    val id: Int,
    val name: String,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
)

@JvmInline
value class ComicVineVolumeId(val id: Int)

fun ProviderSeriesId.toComicVineVolumeId() = ComicVineVolumeId(value.toInt())
