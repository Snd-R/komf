package org.snd.metadata.providers.comicvine.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ComicVineSearchResult<T>(
    val error: String,
    val limit: Int,
    val offset: Int,
    @Json(name = "number_of_page_results")
    val numberOfPageResults: Int,
    @Json(name = "number_of_total_results")
    val numberOfTotalResults: Int,
    @Json(name = "status_code")
    val statusCode: Int,
    val results: T,
    val version: String,
)

