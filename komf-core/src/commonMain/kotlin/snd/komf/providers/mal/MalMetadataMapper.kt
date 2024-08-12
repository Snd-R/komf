package snd.komf.providers.mal

import kotlinx.datetime.LocalDate
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.Image
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType.LOCALIZED
import snd.komf.model.TitleType.NATIVE
import snd.komf.model.TitleType.ROMAJI
import snd.komf.model.WebLink
import snd.komf.model.toReleaseDate
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.mal.model.MalSearchResult
import snd.komf.providers.mal.model.MalSeries
import snd.komf.providers.mal.model.MalStatus

class MalMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    private val mangaBaseUrl = "https://myanimelist.net/manga/"

    fun toSeriesMetadata(series: MalSeries, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            MalStatus.FINISHED -> SeriesStatus.ENDED
            MalStatus.CURRENTLY_PUBLISHING -> SeriesStatus.ONGOING
            MalStatus.NOT_YET_PUBLISHED -> SeriesStatus.ONGOING
            MalStatus.ON_HIATUS -> SeriesStatus.HIATUS
            MalStatus.DISCONTINUED -> SeriesStatus.ABANDONED
        }

        val authors = series.authors.flatMap { author ->
            when (author.role) {
                "Art" -> artistRoles.map { role -> Author("${author.node.firstName} ${author.node.lastName}", role) }
                "Story" -> authorRoles.map { role -> Author("${author.node.firstName} ${author.node.lastName}", role) }
                "Story & Art" ->
                    artistRoles.map { role ->
                        Author("${author.node.firstName} ${author.node.lastName}", role)
                    } + authorRoles.map { role ->
                        Author("${author.node.firstName} ${author.node.lastName}", role)
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
            genres = series.genres.map { it.name },
            authors = authors,
            publisher = null,
            thumbnail = thumbnail,
            tags = emptyList(),
            releaseDate = releaseDate,
            links = listOf(WebLink("MyAnimeList", seriesUrl(series.id))),
            score = series.mean
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.toString()), metadata = metadata),
            metadataConfig
        )
    }

    fun toSeriesSearchResult(searchResult: MalSearchResult): SeriesSearchResult {
        return SeriesSearchResult(
            url = seriesUrl(searchResult.id),
            imageUrl = searchResult.mainPicture?.medium,
            title = searchResult.title,
            provider = CoreProviders.MAL,
            resultId = searchResult.id.toString()
        )
    }

    private fun seriesUrl(id: Int) = mangaBaseUrl + id
}
