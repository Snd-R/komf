package org.snd.metadata.providers.anilist

import org.jsoup.Jsoup
import org.snd.config.SeriesMetadataConfig
import org.snd.fragment.AniListManga
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult
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
import org.snd.type.MediaStatus

class AniListMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val tagsScoreThreshold: Int,
    private val tagsSizeLimit: Int,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    private val mangaLinkBaseUrl = "https://anilist.co/manga/"
    private val allowedRoles = listOf("Story & Art", "Story", "Original Story", "Original Creator", "Art", "Illustration")

    fun toSeriesMetadata(series: AniListManga, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            MediaStatus.FINISHED -> SeriesStatus.ENDED
            MediaStatus.RELEASING -> SeriesStatus.ONGOING
            MediaStatus.NOT_YET_RELEASED -> SeriesStatus.ONGOING
            MediaStatus.CANCELLED -> SeriesStatus.ABANDONED
            MediaStatus.HIATUS -> SeriesStatus.HIATUS
            else -> null
        }

        val authors = series.staff?.edges
            ?.mapNotNull { extractNameAndRole(it) }
            ?.filter { (_, role) -> allowedRoles.contains(role) }
            ?.flatMap { (authorName, aniListRole) ->
                when (aniListRole) {
                    "Story & Art" -> {
                        artistRoles.map { role -> Author(authorName, role) } + authorRoles.map { role -> Author(authorName, role) }
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
            ?.mapNotNull { tag -> tag?.rank?.let { tag.name to tag.rank } }
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
            summary = series.description?.let { Jsoup.parse(it).wholeText() },
            genres = series.genres?.filterNotNull() ?: emptyList(),
            tags = tags,
            authors = authors ?: emptyList(),
            thumbnail = thumbnail,
            totalBookCount = series.volumes,
            releaseDate = ReleaseDate(
                year = series.startDate?.year,
                month = series.startDate?.month,
                day = series.startDate?.day,
            ),
            links = listOf(WebLink("AniList", mangaLinkBaseUrl + series.id)),
            score = series.meanScore?.let { it / 10.0 }
        )
        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.toString()), metadata = metadata),
            metadataConfig
        )
    }

    fun toSearchResult(search: AniListManga): SeriesSearchResult {
        val title = search.title?.english ?: search.title?.romaji ?: search.title?.native
        return SeriesSearchResult(
            imageUrl = search.coverImage?.extraLarge,
            title = title!!,
            provider = Provider.ANILIST,
            resultId = search.id.toString()
        )
    }

    private fun extractNameAndRole(edge: AniListManga.Edge?): Pair<String, String>? {
        if (edge == null) return null

        val role = edge.role?.replace("\\([^)]*\\)".toRegex(), "")?.trim()
        val name = edge.node?.name?.full

        return if (role == null || name == null) null
        else name to role
    }
}
