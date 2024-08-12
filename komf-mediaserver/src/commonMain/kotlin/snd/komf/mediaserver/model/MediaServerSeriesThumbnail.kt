package snd.komf.mediaserver.model

data class MediaServerSeriesThumbnail(
    val id: MediaServerThumbnailId,
    val seriesId: MediaServerSeriesId,
    val type: String?,
    val selected: Boolean,
)
@JvmInline
value class MediaServerThumbnailId(val value: String){
    override fun toString() = value
}
