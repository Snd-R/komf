package snd.komf.providers.bangumi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchSubjectsResponse(
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val data: List<SubjectSearchData>
)

@Serializable
data class SubjectSearchData(
    val id: Long,
    val date: String,
    val image: String,
    val summary: String?,
    val name: String,

    @SerialName("name_cn")
    val nameCn: String,
    val tags: List<SubjectTag>,
    val score: Double,
    val rank: Int,
    val type: SubjectType? = null
)
