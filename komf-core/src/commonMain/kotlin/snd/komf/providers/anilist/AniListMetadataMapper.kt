package snd.komf.providers.anilist

import com.fleeksoft.ksoup.Ksoup
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
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.anilist.model.AniListMedia
import snd.komf.providers.anilist.model.AniListMediaStatus
import snd.komf.providers.anilist.model.AniListStaffEdge

class AniListMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val tagsScoreThreshold: Int,
    private val tagsSizeLimit: Int,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    private val mangaLinkBaseUrl = "https://anilist.co/manga/"
    private val allowedRoles =
        listOf("Story & Art", "Story", "Original Story", "Original Creator", "Art", "Illustration")

    fun toSeriesMetadata(series: AniListMedia, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            AniListMediaStatus.FINISHED -> SeriesStatus.ENDED
            AniListMediaStatus.RELEASING -> SeriesStatus.ONGOING
            AniListMediaStatus.NOT_YET_RELEASED -> SeriesStatus.ONGOING
            AniListMediaStatus.CANCELLED -> SeriesStatus.ABANDONED
            AniListMediaStatus.HIATUS -> SeriesStatus.HIATUS
            else -> null
        }

        val authors = series.staff?.edges
            ?.mapNotNull { extractNameAndRole(it) }
            ?.filter { (_, role) -> allowedRoles.contains(role) }
            ?.flatMap { (authorName, aniListRole) ->
                when (aniListRole) {
                    "Story & Art" -> {
                        artistRoles.map { role -> Author(authorName, role) } + authorRoles.map { role ->
                            Author(
                                authorName,
                                role
                            )
                        }
                    }

                    "Story", "Original Story", "Original Creator" -> {
                        authorRoles.map { role -> Author(authorName, role) }
                    }

                    "Art", "Illustration" -> {
                        artistRoles.map { role -> Author(authorName, role) }
                    }

                    else -> {
                        emptyList()
                    }
                }
            }
        val tags = series.tags?.asSequence()
            ?.mapNotNull { tag -> tag.rank?.let { tag.name to tag.rank } }
            ?.filter { (_, rank) -> rank >= tagsScoreThreshold }
            ?.sortedByDescending { (_, rank) -> rank }
            ?.take(tagsSizeLimit)
            ?.map { (name, _) -> name }
            ?.toList()
            ?: emptyList()

        val titles = listOfNotNull(
            series.title?.english?.let {
                SeriesTitle(name = it.trim(), type = LOCALIZED, language = "en")
            },
            series.title?.romaji?.let {
                SeriesTitle(name = it.trim(), type = ROMAJI, language = "ja-ro")
            },
            series.title?.native?.let {
                SeriesTitle(name = it.trim(), type = NATIVE, language = "ja")
            },
        )

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = series.description?.let { Ksoup.parse(it).wholeText() },
            genres = series.genres ?: emptyList(),
            tags = tags,
            authors = authors ?: emptyList(),
            thumbnail = thumbnail,
            totalBookCount = series.volumes,
            releaseDate = ReleaseDate(
                year = series.startDate?.year,
                month = series.startDate?.month,
                day = series.startDate?.day,
            ),
            links = listOf(WebLink("AniList", seriesUrl(series.id))),
            score = series.meanScore?.let { it / 10.0 }
        )
        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.toString()), metadata = metadata),
            metadataConfig
        )
    }

    fun toSearchResult(search: AniListMedia): SeriesSearchResult {
        val title = search.title?.english ?: search.title?.romaji ?: search.title?.native
        return SeriesSearchResult(
            url = seriesUrl(search.id),
            imageUrl = search.coverImage?.extraLarge,
            title = title!!,
            provider = CoreProviders.ANILIST,
            resultId = search.id.toString()
        )
    }

    private fun seriesUrl(id: Int) = mangaLinkBaseUrl + id

    private fun extractNameAndRole(edge: AniListStaffEdge?): Pair<String, String>? {
        if (edge == null) return null

        val role = edge.role?.replace("\\([^)]*\\)".toRegex(), "")?.trim()
        val name = edge.node?.name?.full

        return if (role == null || name == null) null
        else name to role
    }
}
