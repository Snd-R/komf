package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.model.MediaServerLibrary
import org.snd.mediaserver.model.MediaServerLibraryId
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class KavitaLibrary(
    val id: Int,
    val name: String,
    val lastScanned: LocalDateTime,
    val type: Int,
    val folders: Collection<String>
) {
    fun libraryId() = KavitaLibraryId(id)
}

fun KavitaLibrary.mediaServerLibrary() = MediaServerLibrary(
    id = MediaServerLibraryId(id.toString()),
    name = name,
    roots = folders
)