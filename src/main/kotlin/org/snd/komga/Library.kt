package org.snd.komga

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Library(
    val id: String,
    val name: String,
    val root: String,
    val importComicInfoBook: Boolean,
    val importComicInfoSeries: Boolean,
    val importComicInfoCollection: Boolean,
    val importComicInfoReadList: Boolean,
    val importEpubBook: Boolean,
    val importEpubSeries: Boolean,
    val importMylarSeries: Boolean,
    val importLocalArtwork: Boolean,
    val importBarcodeIsbn: Boolean,
    val scanForceModifiedTime: Boolean,
    val scanDeep: Boolean,
    val repairExtensions: Boolean,
    val convertToCbz: Boolean,
    val emptyTrashAfterScan: Boolean,
    val seriesCover: SeriesCover,
    val hashFiles: Boolean,
    val hashPages: Boolean,
    val analyzeDimensions: Boolean,
    val unavailable: Boolean,
)

enum class SeriesCover {
    FIRST,
    FIRST_UNREAD_OR_FIRST,
    FIRST_UNREAD_OR_LAST,
    LAST,
}
