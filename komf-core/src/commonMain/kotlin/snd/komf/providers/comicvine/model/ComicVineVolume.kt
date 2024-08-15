package snd.komf.providers.comicvine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import snd.komf.model.ProviderSeriesId
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class ComicVineVolumeId(val value: Int) {
    override fun toString() = value.toString()
}

fun ProviderSeriesId.toComicVineVolumeId() = ComicVineVolumeId(value.toInt())

@Serializable
data class ComicVineVolume(
    val id: ComicVineVolumeId,
    val name: String,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String,

    val aliases: String? = null,
    @SerialName("count_of_issues")
    val countOfIssues: Int? = null,
    val description: String? = null,
    val image: ComicVineImage? = null,
    val publisher: ComicVinePublisher? = null,
    @SerialName("start_year")
    val startYear: String? = null,
    @SerialName("resource_type")
    val resourceType: String? = null,
    val characters: List<ComicVineCredit>? = null,
    val locations: List<ComicVineCredit>? = null,
    val issues: List<ComicVineIssueSlim>? = null,
    val concepts: List<ComicVineConcept>? = null,
)

@Serializable
data class ComicVineVolumeSearch(
    val id: Int,
    val name: String,
    @SerialName("api_detail_url")
    val apiDetailUrl: String,
    @SerialName("site_detail_url")
    val siteDetailUrl: String,

    val aliases: String? = null,
    @SerialName("count_of_issues")
    val countOfIssues: Int? = null,
    val description: String? = null,
    @SerialName("first_issue")
    val firstIssue: ComicVineIssueSlim? = null,
    @SerialName("last_issue")
    val lastIssue: ComicVineIssueSlim? = null,
    val image: ComicVineImage? = null,
    val publisher: ComicVinePublisher? = null,
    @SerialName("start_year")
    val startYear: String? = null,
    @SerialName("resource_type")
    val resourceType: String? = null,
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
