package org.snd.metadata.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProviderSeriesMetadata(
    val id: ProviderSeriesId,
    val metadata: SeriesMetadata,
    val books: List<SeriesBook> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SeriesBook(
    val id: ProviderBookId,
    val number: BookRange?,
    val name: String?,
    val type: String?,
    val edition: String?
)
