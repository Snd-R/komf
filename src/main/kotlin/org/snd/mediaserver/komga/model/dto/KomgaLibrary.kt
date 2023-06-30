package org.snd.mediaserver.komga.model.dto

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.model.mediaserver.MediaServerLibrary
import org.snd.mediaserver.model.mediaserver.MediaServerLibraryId

@JsonClass(generateAdapter = true)
data class KomgaLibrary(
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

fun KomgaLibrary.mediaServerLibrary() = MediaServerLibrary(
    id = MediaServerLibraryId(id),
    name = name,
    roots = listOf(root),
)