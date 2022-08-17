package org.snd.metadata.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SeriesSearchResult(
    val imageUrl: String? = null,
    val title: String,
    val provider: Provider,
    val resultId: String,
)
