package org.snd.model

import com.squareup.moshi.JsonClass
import org.snd.metadata.Provider

@JsonClass(generateAdapter = true)
data class SeriesSearchResult(
    val imageUrl: String? = null,
    val title: String,
    val provider: Provider,
    val resultId: String,
)
