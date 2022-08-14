package org.snd.metadata.mal

import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.Provider
import org.snd.metadata.mal.model.Series
import org.snd.metadata.model.AuthorRole
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.Thumbnail

class MalMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
) {
    fun toSeriesMetadata(series: Series, thumbnail: Thumbnail? = null): SeriesMetadata {
        val status = when (series.status) {
            Series.Status.FINISHED -> SeriesMetadata.Status.ENDED
            Series.Status.CURRENTLY_PUBLISHING -> SeriesMetadata.Status.ONGOING
            Series.Status.NOT_YET_PUBLISHED -> SeriesMetadata.Status.ONGOING
            Series.Status.ON_HIATUS -> SeriesMetadata.Status.HIATUS
            Series.Status.DISCONTINUED -> SeriesMetadata.Status.ABANDONED
        }
        val artistRoles = listOf(
            AuthorRole.PENCILLER,
            AuthorRole.INKER,
            AuthorRole.COLORIST,
            AuthorRole.LETTERER,
            AuthorRole.COVER
        )

        val authors = series.authors.flatMap { author ->
            when (author.role) {
                "Art" -> {
                    artistRoles.map { role -> org.snd.metadata.model.Author("${author.firstName} ${author.lastName}", role.name) }
                }
                "Story" -> {
                    listOf(org.snd.metadata.model.Author("${author.firstName} ${author.lastName}", AuthorRole.WRITER.name))
                }
                "Story & Art" -> {
                    artistRoles.map { role ->
                        org.snd.metadata.model.Author(
                            "${author.firstName} ${author.lastName}",
                            role.name
                        )
                    } + org.snd.metadata.model.Author("${author.firstName} ${author.lastName}", AuthorRole.WRITER.name)
                }
                else -> emptyList()
            }
        }

        val metadata = SeriesMetadata(
            status = status,
            title = series.title,
            titleSort = series.title,
            summary = series.synopsis ?: "",
            genres = series.genres,
            authors = authors,
            publisher = "",
            thumbnail = thumbnail,
            tags = emptyList(),

            id = ProviderSeriesId(series.id.toString()),
            provider = Provider.MAL
        )
        return MetadataConfigApplier.apply(metadata, metadataConfig)
    }
}
