package org.snd.metadata.providers.mangaupdates.model.json

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResultPageJson(
    val total_hits: Int,
    val page: Int,
    val per_page: Int,
    val results: Collection<ResultJson>
)

@JsonClass(generateAdapter = true)
data class ResultJson(
    val record: SearchResultJson,
    val hit_title: String?,
)

@JsonClass(generateAdapter = true)
data class SearchResultJson(
    val series_id: Long,
    val title: String,
    val description: String?,
    val image: ImageJson?,
    val genres: Collection<GenreJson>?,
    val year: String?,
)

@JsonClass(generateAdapter = true)
data class ImageJson(val url: UrlJson)

@JsonClass(generateAdapter = true)
data class UrlJson(val original: String?, val thumb: String?)

@JsonClass(generateAdapter = true)
data class GenreJson(val genre: String)
