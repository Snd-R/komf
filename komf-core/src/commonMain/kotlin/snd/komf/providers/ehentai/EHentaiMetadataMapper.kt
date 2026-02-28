package snd.komf.providers.ehentai

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.Image
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesTitle
import snd.komf.model.WebLink
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.ehentai.model.EHentaiBook

class EHentaiMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(
        book: EHentaiBook,
        thumbnail: Image?
    ): ProviderSeriesMetadata {
        val rawTags = book.tags ?: emptyList()

        val languageTag = rawTags.firstOrNull { it.startsWith("language:") }
        val language = when (languageTag) {
            "language:english" -> "en"
            "language:chinese" -> "zh"
            "language:japanese" -> "ja"
            "language:korean" -> "ko"
            "language:russian" -> "ru"
            "language:french" -> "fr"
            "language:spanish" -> "es"
            else -> null
        }

        val artists = rawTags.filter { it.startsWith("artist:") }.map { it.removePrefix("artist:") }
        val groups = rawTags.filter { it.startsWith("group:") }.map { it.removePrefix("group:") }
        val authors = (artists + groups).flatMap { authorName ->
            authorRoles.map { role -> Author(authorName, role) }
        }

        val finalTags = rawTags
            .asSequence()
            .filterNot { it.startsWith("language:") }
            .filterNot { it.startsWith("artist:") }
            .filterNot { it.startsWith("group:") }
            .map { tag -> if (tag.contains(":")) tag.substringAfter(":") else tag }
            .distinct()
            .toList()

        val titles = listOfNotNull(
            book.title?.let { SeriesTitle(name = it, type = null, language = null) },
            book.titleJpn?.ifBlank { null }?.let { SeriesTitle(name = it, type = null, language = "ja") }
        )

        val token = book.token ?: "unknown"
        val link = WebLink("e-hentai", "https://e-hentai.org/g/${book.gid}/$token")

        val metadata = SeriesMetadata(
            titles = titles,
            language = language,
            releaseDate = book.posted?.toLocalDateTime(TimeZone.UTC)?.let { date ->
                ReleaseDate(
                    year = date.year,
                    month = date.month.number,
                    day = date.day
                )
            },
            tags = finalTags,
            authors = authors,
            links = listOf(link),
            thumbnail = thumbnail
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(
                id = ProviderSeriesId("${book.gid};$token"),
                metadata = metadata
            ),
            metadataConfig
        )
    }

    fun toSeriesSearchResult(result: EHentaiBook): SeriesSearchResult {
        val token = result.token ?: "unknown"
        return SeriesSearchResult(
            resultId = "${result.gid};$token",
            url = "https://e-hentai.org/g/${result.gid}/$token",
            imageUrl = result.thumb,
            title = result.titleJpn?.ifBlank { null } ?: result.title ?: "Unknown Title",
            provider = CoreProviders.EHENTAI
        )
    }
}