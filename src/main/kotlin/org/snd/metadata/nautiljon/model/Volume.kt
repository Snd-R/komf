package org.snd.metadata.nautiljon.model

import org.snd.metadata.model.Thumbnail
import org.snd.metadata.model.VolumeMetadata
import java.time.LocalDate

data class Volume(
    val id: VolumeId,
    val number: Int,
    val originalPublisher: String?,
    val frenchPublisher: String?,
    val originalReleaseDate: LocalDate?,
    val frenchReleaseDate: LocalDate?,
    val numberOfPages: Int?,
    val description: String?,
    val score: Double?,
    val imageUrl: String?,
    val chapters: Collection<Chapter>,
    val authorsStory: Collection<String>,
    val authorsArt: Collection<String>,
)

data class Chapter(val name: String?, val number: Int)

fun Volume.toBookMetadata(thumbnail: Thumbnail? = null): VolumeMetadata {
    val authors = authorsStory.map { org.snd.metadata.model.Author(it, "writer") } +
            authorsArt.map { org.snd.metadata.model.Author(it, "artist") }

    return VolumeMetadata(
        summary = description,
        number = number,
        releaseDate = originalReleaseDate,
        authors = authors,
        startChapter = null,
        endChapter = null,

        thumbnail = thumbnail
    )
}
