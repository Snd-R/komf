package org.snd.metadata.providers.comicvine.model

import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
class ComicVineResult<T>(
    val error: String,
    val limit: Int,
    val offset: Int,
    val number_of_page_results: Int,
    val number_of_total_results: Int,
    val status_code: Int,
    val results: T,
    val version: String,
)

@JsonClass(generateAdapter = true)
data class ComicVineVolume(
    val id: Int,
    val name: String,
    val api_detail_url: String,
    val site_detail_url: String,

    val aliases: String?,
    val count_of_issues: Int?,
    val description: String?,
    val image: ComicVineImage?,
    val publisher: ComicVinePublisher?,
    val start_year: String?,
    val resource_type: String?,
    val characters: List<ComicVineCredit>?,
    val locations: List<ComicVineCredit>?,
    val issues: List<ComicVineIssue>?,
    val concepts: List<ComicVineConcept>?,
)

@JsonClass(generateAdapter = true)
data class ComicVineIssue(
    val id: Int,
    val name: String,
    val api_detail_url: String,
    val site_detail_url: String,

    val aliases: String?,
    val associated_images: List<ComicVineAltImage>?,
    val character_credits: List<ComicVineCredit>?,
    val concept_credits: List<ComicVineCredit>?,
    val cover_date: String?,
    val date_added: String?,
    val date_last_updated: String?,
    val description: String?,
    val has_staff_review: Boolean?,
    val image: ComicVineImage?,
    val issue_number: String?,
    val location_credits: List<ComicVineCredit>?,
    val object_credits: List<ComicVineCredit>?,
    val person_credits: List<ComicVinePersonCredit>?,
    val store_date: String?,
    val story_arc_credits: List<ComicVineCredit>?,

    val team_credits: List<ComicVineCredit>?,
    val volume: ComicVineVolume?,
)

@JsonClass(generateAdapter = true)
data class ComicVineImage(
    val icon_url: String,
    val medium_url: String,
    val screen_url: String,
    val screen_large_url: String,
    val small_url: String,
    val super_url: String,
    val thumb_url: String,
    val tiny_url: String,
    val original_url: String,
    val image_tags: String,
)

@JsonClass(generateAdapter = true)
data class ComicVineAltImage(
    val id: Int,
    val original_url: String?,
    val caption: String?,
    val image_tags: String?,
)

@JsonClass(generateAdapter = true)
data class ComicVinePublisher(
    val id: Int,
    val name: String,
    val api_detail_url: String,
)

@JsonClass(generateAdapter = true)
data class ComicVineCredit(
    val id: Int,
    val name: String,
    val api_detail_url: String,
    val site_detail_url: String,
)

@JsonClass(generateAdapter = true)
data class ComicVineConcept(
    val id: Int,
    val name: String,
    val api_detail_url: String,
    val site_detail_url: String,
    val count: String,
)

@JsonClass(generateAdapter = true)
data class ComicVinePersonCredit(
    val id: Int,
    val name: String,
    val api_detail_url: String,
    val site_detail_url: String,
    val role: String,
)

