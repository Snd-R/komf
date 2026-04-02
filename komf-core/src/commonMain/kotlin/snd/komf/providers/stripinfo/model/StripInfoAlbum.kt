package snd.komf.providers.stripinfo.model

data class StripInfoAlbum(
    val id: StripInfoAlbumId,
    val seriesId: StripInfoSeriesId?,
    val title: String?,
    val number: String?,
    val year: Int?,
    val pageCount: Int?,
    val isbn: String?,
    val barcode: String?,
    val language: String?,
    val binding: String?,
    val colorInfo: String?,
    val publisher: String?,
    val imageUrl: String?,
    val credits: List<StripInfoCredit>,
)

data class StripInfoCredit(
    val name: String,
    val role: StripInfoRole,
    val authorId: Int?,
)

enum class StripInfoRole {
    SCENARIO,
    TEKENINGEN,
    KLEUREN,
    COVER,
}
