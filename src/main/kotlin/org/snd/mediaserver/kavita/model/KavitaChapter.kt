package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.model.MediaServerBook
import org.snd.mediaserver.model.MediaServerBookId
import org.snd.mediaserver.model.MediaServerSeriesId
import java.nio.file.Path
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class KavitaChapter(
    val id: Int,
    val range: String?,
    val number: String?,
    val pages: Int,
    val isSpecial: Boolean,
    val title: String,
    val files: Collection<KavitaChapterFile>,
    val pagesRead: Int,
    val coverImageLocked: Boolean,
    val volumeId: Int,
    val created: LocalDateTime,
    val releaseDate: LocalDateTime,
    val titleName: String?,
    val summary: String?,
    val ageRating: Int,
) {
    fun chapterId() = KavitaChapterId(id)
    fun volumeId() = KavitaVolumeId(volumeId)
}

@JsonClass(generateAdapter = true)
data class KavitaChapterFile(
    val id: Int,
    val filePath: String,
    val pages: String,
    val format: Int,
    val created: LocalDateTime
)

fun KavitaChapter.mediaServerBook(volume: KavitaVolume, metadata: KavitaChapterMetadata): MediaServerBook {
    val filePath = Path.of(files.first().filePath)
    val fileName = filePath.fileName.toString()

    return MediaServerBook(
        id = MediaServerBookId(id.toString()),
        seriesId = MediaServerSeriesId(volume.seriesId.toString()),
        libraryId = null,
        seriesTitle = title,
        name = fileName,
        url = filePath.toString(),
        number = number?.toIntOrNull() ?: 0,
        metadata = metadata.mediaServerBookMetadata(this),
        deleted = false,
    )
}
