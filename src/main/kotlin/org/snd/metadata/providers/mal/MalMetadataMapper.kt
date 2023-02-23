package org.snd.metadata.providers.mal

import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.metadata.Author
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.ReleaseDate
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesStatus
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.TitleType.LOCALIZED
import org.snd.metadata.model.metadata.TitleType.NATIVE
import org.snd.metadata.model.metadata.TitleType.ROMAJI
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.model.metadata.toReleaseDate
import org.snd.metadata.providers.mal.model.Series
import java.time.LocalDate

class MalMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
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

        val authors = series.authors.flatMap { author ->
            when (author.role) {
                "Art" -> {
                    artistRoles.map { role -> Author("${author.firstName} ${author.lastName}", role) }
                }

                "Story" -> {
                    authorRoles.map { role -> Author("${author.firstName} ${author.lastName}", role) }
                }

                "Story & Art" -> {
                    artistRoles.map { role ->
                        Author("${author.firstName} ${author.lastName}", role)
                    } + authorRoles.map { role ->
                        Author("${author.firstName} ${author.lastName}", role)
                    }
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
            SeriesTitle(series.title, ROMAJI, "ja-ro"),
            series.alternativeTitles?.en?.let { if (it.isNotBlank()) SeriesTitle(it, LOCALIZED, "en") else null },
            series.alternativeTitles?.ja?.let { if (it.isNotBlank()) SeriesTitle(it, NATIVE, "ja") else null })

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
            links = listOf(WebLink("MyAnimeList", mangaBaseUrl + series.id)),
            score = series.mean
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.toString()), metadata = metadata),
            metadataConfig
        )
    }
}
