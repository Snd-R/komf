package org.snd.metadata.nautiljon.model

import org.snd.metadata.Provider
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.Thumbnail
import org.snd.metadata.model.VolumeMetadata
import java.time.Year

data class Series(
    val id: SeriesId,
    val title: String,
    val alternativeTitles: Collection<String>,
    val originalTitles: Collection<String>,
    val description: String?,
    val imageUrl: String?,
    val country: String?,
    val type: String?,
    val startYear: Year?,
    val status: String?,
    val numberOfVolumes: Int?,
    val genres: Collection<String>,
    val themes: Collection<String>,
    val authorsStory: Collection<String>,
    val authorsArt: Collection<String>,
    val originalPublisher: String?,
    val frenchPublisher: String?,
    val score: Double?,

    val volumeIds: Collection<VolumeId>
)

fun Series.toSeriesMetadata(volumeMetadata: List<VolumeMetadata>, thumbnail: Thumbnail? = null): SeriesMetadata {
    val status = when (status) {
        "En cours" -> SeriesMetadata.Status.ONGOING
        "En attente" -> SeriesMetadata.Status.ONGOING
        "Terminé" -> SeriesMetadata.Status.ENDED
        else -> null
    }

    val authors = authorsStory.map { org.snd.metadata.model.Author(it, "writer") } +
            authorsArt.map { org.snd.metadata.model.Author(it, "artist") }

    return SeriesMetadata(
        id = ProviderSeriesId(id.id),
        provider = Provider.NAUTILJON,

        status = status,
        title = title,
        titleSort = title,
        summary = description,
        publisher = originalPublisher,
        genres = genres,
        tags = themes,
        authors = authors,
        thumbnail = thumbnail,
        totalBookCount = numberOfVolumes,

        volumeMetadata = volumeMetadata
    )
}
