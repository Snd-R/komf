package snd.komf.mediaserver.model

data class MediaServerLibrary(
    val id: MediaServerLibraryId,
    val name: String,
    val roots: Collection<String>,
)
@JvmInline
value class MediaServerLibraryId(val value: String)
