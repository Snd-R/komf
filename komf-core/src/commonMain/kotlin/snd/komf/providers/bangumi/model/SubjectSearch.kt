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
    val image: String? = null,
    val summary: String? = null,
    val name: String? = null,

    @SerialName("name_cn")
    val nameCn: String? = null,
    val tags: List<SubjectTag> = emptyList(),
    val rating: SubjectRating? = null,
    val type: SubjectType? = null
)
