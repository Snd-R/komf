package snd.komf.mediaserver.model

data class MediaServerSeries(
    val id: MediaServerSeriesId,
    val libraryId: MediaServerLibraryId,
    val name: String,
    val booksCount: Int,
    val metadata: MediaServerSeriesMetadata,
    val url: String,
    val deleted: Boolean,
)

@JvmInline
value class MediaServerSeriesId(val value: String) {
    override fun toString() = value
}
