package snd.komf.mediaserver.kavita.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import snd.komf.mediaserver.model.MediaServerLibraryId


@JvmInline
@Serializable
value class KavitaLibraryId(val value: Int)

fun MediaServerLibraryId.toKavitaLibraryId() = KavitaLibraryId(value.toInt())
@Serializable
data class KavitaLibrary(
    val id: KavitaLibraryId,
    val name: String,
    val lastScanned: LocalDateTime,
    val type: Int,
    val folders: Collection<String>
)
