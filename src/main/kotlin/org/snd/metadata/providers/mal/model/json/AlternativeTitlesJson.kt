package org.snd.metadata.providers.mal.model.json

import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class AlternativeTitlesJson(
    val synonyms: List<String>,
    val en: String,
    val ja: String
)
