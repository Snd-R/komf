package org.snd.metadata.model

data class ProviderSeriesMetadata(
    val id: ProviderSeriesId,
    val metadata: SeriesMetadata,
    val books: List<SeriesBook> = emptyList(),
)

data class SeriesBook(
    val id: ProviderBookId,
    val number: Int?,
    val name: String?,
    val type: String?,
    val edition: String?
)
