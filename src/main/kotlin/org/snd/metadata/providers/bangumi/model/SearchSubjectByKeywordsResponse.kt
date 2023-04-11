package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchSubjectByKeywordsResponse(
    val results: Int? = null,
    val list: List<Subject>? = null,
)