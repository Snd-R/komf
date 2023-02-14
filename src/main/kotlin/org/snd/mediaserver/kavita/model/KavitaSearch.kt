package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.model.mediaserver.MediaServerLibraryId
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesId
import org.snd.mediaserver.model.mediaserver.MediaServerSeriesSearch

@JsonClass(generateAdapter = true)
data class KavitaSearch(
    val series: Collection<KavitaSeriesSearch>
)

@JsonClass(generateAdapter = true)
data class KavitaSeriesSearch(
    val seriesId: Int,
    val name: String,
    val originalName: String,
    val sortName: String,
    val localizedName: String,
    val format: Int,
    val libraryName: String,
    val libraryId: Int
)

fun KavitaSeriesSearch.mediaServerSeriesSearch() = MediaServerSeriesSearch(
    id = MediaServerSeriesId(seriesId.toString()),
    libraryId = MediaServerLibraryId(libraryId.toString()),
    name = name,
)