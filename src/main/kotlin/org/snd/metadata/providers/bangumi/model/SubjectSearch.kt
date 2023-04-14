package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchSubjectsResponse(
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val data: List<SubjectSearchData>
)

@JsonClass(generateAdapter = true)
data class SubjectSearchData(
    val id: Long,
    val date: String,
    val image: String,
    val summary: String?,
    val name: String,

    @Json(name = "name_cn")
    val nameCn: String,
    val tags: List<SubjectTag>,
    val score: Double,
    val rank: Int,
    val type: SubjectType? = null
)
