package org.snd.metadata.providers.comicvine.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ComicVineCredit(
    val id: Int,
    val name: String,
    @Json(name = "api_detail_url")
    val apiDetailUrl: String,
    @Json(name = "site_detail_url")
    val siteDetailUrl: String,
)

