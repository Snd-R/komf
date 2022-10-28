package org.snd.metadata.mylar.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MylarMetadata(
    val type: String,
    val publisher: String,
    val imprint: String?,
    val name: String,
    val comicid: String?,
    val cid: String,
    val year: Int,

    @Json(name = "description_text")
    val descriptionText: String?,

    @Json(name = "description_formatted")
    val descriptionFormatted: String?,
    val volume: Int?,

    @Json(name = "booktype")
    val bookType: String,

    @Json(name = "age_rating")
    val ageRating: String?,

    @Json(name = "ComicImage")
    val comicImage: String,

    @Json(name = "total_issues")
    val totalIssues: Int,

    @Json(name = "publication_run")
    val publicationRun: String,

    val status: MylarStatus?,

    val collects: Collection<Map<String, Any>>,
)
