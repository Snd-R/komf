package snd.komf.providers.stripinfo.model

data class StripInfoSeries(
    val id: StripInfoSeriesId,
    val title: String,
    val imageUrl: String?,
    val startYear: Int?,
    val endYear: Int?,
    val rating: Double?,
    val ratingCount: Int?,
    val authors: List<String>,
    val publishers: List<String>,
    val tags: List<String>,
    val language: String?,
    val albums: List<StripInfoSeriesAlbum>,
)

data class StripInfoSeriesAlbum(
    val id: StripInfoAlbumId,
    val number: String?,
    val title: String?,
    val yearRange: String?,
)
