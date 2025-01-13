package snd.komf.api.mediaserver

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class KomfMediaServerLibrary(
    val id: KomfMediaServerLibraryId,
    val name: String,
    val roots: Collection<String>,
)

@JvmInline
@Serializable
value class KomfMediaServerLibraryId(val value: String)
