package snd.komf.mediaserver.model

data class MediaServerBook(
    val id: MediaServerBookId,
    val seriesId: MediaServerSeriesId,
    val libraryId: MediaServerLibraryId?,
    val seriesTitle: String,
    val name: String,
    val url: String,
    val number: Int,
    val oneshot: Boolean,
    val metadata: MediaServerBookMetadata,
    val deleted: Boolean,
)

@JvmInline
value class MediaServerBookId(val value: String) {
    override fun toString() = value
}
