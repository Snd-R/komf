package org.snd.metadata.providers.mal.model.json

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResultsJson(
    val data: List<SearchNodeJson>,
    val paging: PagingJson
)

@JsonClass(generateAdapter = true)
data class SearchNodeJson(
    val node: SearchResultJson,
)

@JsonClass(generateAdapter = true)
data class SearchResultJson(
    val id: Int,
    val title: String,
    val alternative_titles: AlternativeTitlesJson,
    val main_picture: PictureJson? = null,
)


@JsonClass(generateAdapter = true)
data class PagingJson(
    val next: String
)
