package org.snd.metadata.nautiljon

import org.snd.metadata.Provider
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.model.AuthorRole
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.Thumbnail
import org.snd.metadata.model.VolumeMetadata
import org.snd.metadata.nautiljon.model.Series
import org.snd.metadata.nautiljon.model.Volume

class NautiljonSeriesMetadataMapper(
    private val useOriginalPublisher: Boolean,
    private val originalPublisherTag: String?,
    private val frenchPublisherTag: String?
) {
    private val artistRoles = listOf(
        AuthorRole.PENCILLER,
        AuthorRole.INKER,
        AuthorRole.COLORIST,
        AuthorRole.LETTERER,
        AuthorRole.COVER
    )

    fun toSeriesMetadata(series: Series, volumeMetadata: List<VolumeMetadata>, thumbnail: Thumbnail? = null): SeriesMetadata {
        val status = when (series.status) {
            "En cours" -> SeriesMetadata.Status.ONGOING
            "En attente" -> SeriesMetadata.Status.ONGOING
            "Terminé" -> SeriesMetadata.Status.ENDED
            else -> SeriesMetadata.Status.ONGOING
        }


        val authors = series.authorsStory.map { org.snd.metadata.model.Author(it, AuthorRole.WRITER.name) } +
                series.authorsArt.flatMap { artist -> artistRoles.map { role -> org.snd.metadata.model.Author(artist, role.name) } }

        val tags = series.themes + listOfNotNull(
            originalPublisherTag?.let { "$it: ${series.originalPublisher}" },
            frenchPublisherTag?.let { "$it: ${series.frenchPublisher}" }
        )

        return SeriesMetadata(
            id = ProviderSeriesId(series.id.id),
            provider = Provider.NAUTILJON,

            status = status,
            title = series.title,
            titleSort = series.title,
            summary = series.description ?: "",
            publisher = (if (useOriginalPublisher) series.originalPublisher else series.frenchPublisher) ?: "",
            genres = series.genres,
            tags = tags,
            authors = authors,
            thumbnail = thumbnail,
            totalBookCount = series.numberOfVolumes,
            ageRating = series.recommendedAge,

            volumeMetadata = volumeMetadata
        )
    }

    fun toBookMetadata(volume: Volume, thumbnail: Thumbnail? = null): VolumeMetadata {
        val authors = volume.authorsStory.map { org.snd.metadata.model.Author(it, AuthorRole.WRITER.name) } +
                volume.authorsArt.flatMap { artist -> artistRoles.map { role -> org.snd.metadata.model.Author(artist, role.name) } }

        return VolumeMetadata(
            summary = volume.description ?: "",
            number = volume.number,
            releaseDate = if (useOriginalPublisher) volume.originalReleaseDate else volume.frenchReleaseDate,
            authors = authors,
            startChapter = null,
            endChapter = null,

            thumbnail = thumbnail
        )
    }
}