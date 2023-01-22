package org.snd.metadata.providers.mal

import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Author
import org.snd.metadata.model.AuthorRole
import org.snd.metadata.model.Image
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.ReleaseDate
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesStatus
import org.snd.metadata.model.SeriesTitle
import org.snd.metadata.model.TitleType.LOCALIZED
import org.snd.metadata.model.TitleType.NATIVE
import org.snd.metadata.model.TitleType.ROMAJI
import org.snd.metadata.model.WebLink
import org.snd.metadata.model.toReleaseDate
import org.snd.metadata.providers.mal.model.Series
import java.time.LocalDate

class MalMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
) {
    private val mangaBaseUrl = "https://myanimelist.net/manga/"

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
                    artistRoles.map { role -> Author("${author.firstName} ${author.lastName}".trim(), role) }
                }

                "Story" -> {
                    listOf(Author("${author.firstName} ${author.lastName}".trim(), AuthorRole.WRITER))
                }

                "Story & Art" -> {
                    artistRoles.map { role ->
                        Author(
                            "${author.firstName} ${author.lastName}".trim(),
                            role
                        )
                    } + Author("${author.firstName} ${author.lastName}".trim(), AuthorRole.WRITER)
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
            series.alternativeTitles?.en?.let { if (it.isNotBlank()) SeriesTitle(it, LOCALIZED) else null },
            series.alternativeTitles?.ja?.let { if (it.isNotBlank()) SeriesTitle(it, NATIVE) else null })

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = series.synopsis,
            genres = series.genres,
            authors = authors,
            publisher = null,
            thumbnail = thumbnail,
            tags = emptyList(),
            releaseDate = releaseDate,
            links = listOf(WebLink("MyAnimeList", mangaBaseUrl + series.id))
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.toString()), metadata = metadata),
            metadataConfig
        )
    }
}
