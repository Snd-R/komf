package org.snd.metadata.anilist

import org.jsoup.Jsoup
import org.snd.fragment.AniListManga
import org.snd.metadata.Provider
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.model.Author
import org.snd.metadata.model.AuthorRole
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.Thumbnail
import org.snd.type.MediaStatus

class AniListMetadataMapper {
    private val allowedRoles = listOf("Story & Art", "Story", "Art", "Illustration")

    private val artistRoles = listOf(
        AuthorRole.PENCILLER,
        AuthorRole.INKER,
        AuthorRole.COLORIST,
        AuthorRole.LETTERER,
        AuthorRole.COVER
    )

    fun toSeriesMetadata(series: AniListManga, thumbnail: Thumbnail? = null): SeriesMetadata {
        val status = when (series.status) {
            MediaStatus.FINISHED -> SeriesMetadata.Status.ENDED
            MediaStatus.RELEASING -> SeriesMetadata.Status.ONGOING
            MediaStatus.NOT_YET_RELEASED -> SeriesMetadata.Status.ONGOING
            MediaStatus.CANCELLED -> SeriesMetadata.Status.ABANDONED
            MediaStatus.HIATUS -> SeriesMetadata.Status.HIATUS
            else -> null
        }

        val authors = series.staff?.edges
            ?.filter { allowedRoles.contains(it?.role) }
            ?.filter { it?.node?.languageV2 == "Japanese" }
            ?.mapNotNull {
                val name = it?.node?.name?.full
                val role = it?.role

                if (name == null || role == null) null
                else role to name
            }
            ?.flatMap {
                when (it.first) {
                    "Story & Art" -> {
                        artistRoles.map { role -> Author(it.second, role.name) } + Author(it.second, AuthorRole.WRITER.name)
                    }
                    "Story" -> {
                        listOf(Author(it.second, AuthorRole.WRITER.name))
                    }
                    "Art", "Illustration" -> {
                        artistRoles.map { role -> Author(it.second, role.name) }
                    }
                    else -> {
                        emptyList()
                    }
                }
            }
        val allTags = series.tags?.filterNotNull()
            ?.mapNotNull { if (it.rank == null) null else it.name to it.rank } ?: emptyList()

        val tags = if (allTags.size <= 15) {
            allTags.sortedBy { it.second }.map { it.first }
        } else if (allTags.count { it.second > 70 } <= 15) {
            allTags.sortedBy { it.second }.take(15).map { it.first }
        } else {
            allTags.filter { it.second > 70 }.map { it.first }
        }


        return SeriesMetadata(
            id = ProviderSeriesId(series.id.toString()),
            provider = Provider.ANILIST,

            status = status,
            title = series.title?.english,
            titleSort = series.title?.english,
            summary = series.description?.let { Jsoup.parse(it).wholeText() },
            genres = series.genres?.filterNotNull(),
            tags = tags,
            authors = authors,
            thumbnail = thumbnail,
            totalBookCount = series.volumes,
        )
    }

    fun toSearchResult(search: AniListManga): SeriesSearchResult {
        val title = search.title?.english ?: search.title?.romaji ?: search.title?.native
        return SeriesSearchResult(
            imageUrl = search.coverImage?.large,
            title = title!!,
            provider = Provider.ANILIST,
            resultId = search.id.toString()
        )
    }
}
