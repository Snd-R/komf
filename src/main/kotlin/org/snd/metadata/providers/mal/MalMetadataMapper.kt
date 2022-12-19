package org.snd.metadata.providers.mal

import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.*
import org.snd.metadata.model.TitleType.*
import org.snd.metadata.providers.mal.model.Series
import java.time.LocalDate

class MalMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
) {
    fun toSeriesMetadata(series: Series, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            Series.Status.FINISHED -> SeriesStatus.ENDED
            Series.Status.CURRENTLY_PUBLISHING -> SeriesStatus.ONGOING
            Series.Status.NOT_YET_PUBLISHED -> SeriesStatus.ONGOING
            Series.Status.ON_HIATUS -> SeriesStatus.HIATUS
            Series.Status.DISCONTINUED -> SeriesStatus.ABANDONED
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
                    artistRoles.map { role -> Author("${author.firstName} ${author.lastName}", role) }
                }

                "Story" -> {
                    listOf(Author("${author.firstName} ${author.lastName}", AuthorRole.WRITER))
                }

                "Story & Art" -> {
                    artistRoles.map { role ->
                        Author(
                            "${author.firstName} ${author.lastName}",
                            role
                        )
                    } + Author("${author.firstName} ${author.lastName}", AuthorRole.WRITER)
                }

                else -> emptyList()
            }
        }
        val releaseDate = try {
            series.startDate?.let {
                it.toIntOrNull()
                    ?.let { year -> ReleaseDate(year, null, null) }
                    ?: LocalDate.parse(series.startDate).toReleaseDate()
            }
        } catch (e: Exception) {
            null
        }

        val titles = listOfNotNull(
            SeriesTitle(series.title, ROMAJI),
            series.alternativeTitles?.en?.let { SeriesTitle(it, LOCALIZED) },
            series.alternativeTitles?.ja?.let { SeriesTitle(it, NATIVE) })

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = series.synopsis,
            genres = series.genres,
            authors = authors,
            publisher = null,
            thumbnail = thumbnail,
            tags = emptyList(),
            releaseDate = releaseDate
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.toString()), metadata = metadata),
            metadataConfig
        )
    }
}
